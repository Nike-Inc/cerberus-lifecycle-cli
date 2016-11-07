/*
 * Copyright (c) 2016 Nike Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.operation.gateway;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.LogType;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.gateway.CreateCloudFrontSecurityGroupUpdaterLambdaCommand;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.cloudformation.CloudFrontIpSynchronizerOutputs;
import com.nike.cerberus.domain.cloudformation.CloudFrontIpSynchronizerParameters;
import com.nike.cerberus.domain.environment.LambdaName;
import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.UnexpectedCloudFormationStatusException;
import com.nike.cerberus.service.CloudFormationService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.nike.cerberus.module.CerberusModule.CF_OBJECT_MAPPER;

/**
 * Command for creating lambda needed to sync AWS CloudFront IPs to an SG to limit ingress to only CF IPs
 */
public class CreateCloudFrontSecurityGroupUpdaterLambdaOperation implements Operation<CreateCloudFrontSecurityGroupUpdaterLambdaCommand> {

    private static final String AWS_IP_CHANGE_TOPIC_ARN = "arn:aws:sns:us-east-1:806199016981:AmazonIpSpaceChanged";
    private static final String BAD_HASH = "03a8199d0c03ddfec0e542f8bf650ee7";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CloudFormationService cloudFormationService;
    private final AmazonSNS amazonSNS;
    private final ObjectMapper cloudformationObjectMapper;
    private final EnvironmentMetadata environmentMetadata;
    private final AWSLambda awsLambda;
    private final AmazonS3 amazonS3;

    @Inject
    public CreateCloudFrontSecurityGroupUpdaterLambdaOperation(final CloudFormationService cloudFormationService,
                                                               final EnvironmentMetadata environmentMetadata,
                                                               @Named(CF_OBJECT_MAPPER) final ObjectMapper cloudformationObjectMapper,
                                                               AWSLambda awsLambda,
                                                               AmazonS3 amazonS3) {

        this.cloudFormationService = cloudFormationService;
        this.cloudformationObjectMapper = cloudformationObjectMapper;
        this.environmentMetadata = environmentMetadata;
        this.awsLambda = awsLambda;
        this.amazonS3 = amazonS3;

        final Region region = Region.getRegion(Regions.US_EAST_1);
        AmazonCloudFormation amazonCloudFormation = new AmazonCloudFormationClient();
        amazonCloudFormation.setRegion(region);
        amazonSNS = new AmazonSNSClient();
        amazonSNS.setRegion(region);
    }

    @Override
    public void run(CreateCloudFrontSecurityGroupUpdaterLambdaCommand command) {
        if (! cloudFormationService.isStackPresent(StackName.CLOUD_FRONT_IP_SYNCHRONIZER.getName())) {
            createLambda();
        }

        Map<String, String> outputs = cloudFormationService.getStackOutputs(StackName.CLOUD_FRONT_IP_SYNCHRONIZER.getName());
        CloudFrontIpSynchronizerOutputs cloudFrontIpSynchronizerOutputs = cloudformationObjectMapper
                .convertValue(outputs, CloudFrontIpSynchronizerOutputs.class);

        final String arn = cloudFrontIpSynchronizerOutputs.getCloudFrontOriginElbSgIpSyncFunctionArn();

        // subscribe, if already subscribed it doesn't make a new sub
        amazonSNS.subscribe(new SubscribeRequest(AWS_IP_CHANGE_TOPIC_ARN, "lambda", arn));

        // force any new ELBs that have the tags we care about to be updated to only allow ingress from CloudFront
        forceLambdaToUpdateSgs(arn);
    }

    /**
     * Forces the lambda to run and sync the IPs for CloudFront to be white listed on the origin elb
     */
    private void forceLambdaToUpdateSgs(String arn) {
        String json;
        try {
             json = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("aws-ip-space-change-sns-sample-event.json"));
        } catch (IOException e) {
            String msg = "Failed to load mock sns message, to force Lambda first run";
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
        // this will fail
        InvokeResult result = awsLambda.invoke(new InvokeRequest().withFunctionName(arn).withPayload(String.format(json, BAD_HASH)).withLogType(LogType.Tail));
        // collect the error so we can parse it for the latest hash
        String log = new String(Base64.getDecoder().decode(result.getLogResult()));
        Pattern pattern = Pattern.compile("MD5 Mismatch: got\\s(.*?)\\sexp.*?");
        Matcher matcher = pattern.matcher(log);
        boolean matched = matcher.find();
        if (! matched) {
            throw new RuntimeException("failed to extract hash from: " + log);
        }

        String realHash = matcher.group(1);
        result = awsLambda.invoke(new InvokeRequest().withFunctionName(arn).withPayload(String.format(json, realHash)).withLogType(LogType.Tail));

        logger.info("Forcing the Lambda to run and update Security Groups");
        logger.info(new String(result.getPayload().array()));
    }

    private void createLambda() {
        final CloudFrontIpSynchronizerParameters cloudFrontIpSynchronizerParameters = new CloudFrontIpSynchronizerParameters()
                .setLambdaBucket(environmentMetadata.getBucketName())
                .setLambdaKey(LambdaName.CLOUD_FRONT_SG_GROUP_IP_SYNC.getBucketKey());

        final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {};
        final Map<String, String> parameters = cloudformationObjectMapper.convertValue(cloudFrontIpSynchronizerParameters, typeReference);

        final String stackId = cloudFormationService.createStack(StackName.CLOUD_FRONT_IP_SYNCHRONIZER.getName(),
                parameters, ConfigConstants.CF_ELB_IP_SYNC_STACK_TEMPLATE_PATH, true);

        final StackStatus endStatus =
                cloudFormationService.waitForStatus(stackId,
                        Sets.newHashSet(StackStatus.CREATE_COMPLETE, StackStatus.ROLLBACK_COMPLETE));

        if (endStatus != StackStatus.CREATE_COMPLETE) {
            final String errorMessage = String.format("Unexpected end status: %s", endStatus.name());
            logger.error(errorMessage);

            throw new UnexpectedCloudFormationStatusException(errorMessage);
        }
    }

    /**
     * Run the command if the stack already exists from a different Cerberus env or if the publish artifact command
     * has been run for this env, so that if were creating the stack the artifact we need exists in s3.
     */
    @Override
    public boolean isRunnable(CreateCloudFrontSecurityGroupUpdaterLambdaCommand command) {
        boolean theStackAlreadyExists = cloudFormationService
                .isStackPresent(StackName.CLOUD_FRONT_IP_SYNCHRONIZER.getName());

        boolean theLambdaArtifactExistsInS3 = amazonS3.doesObjectExist(environmentMetadata.getBucketName(),
                LambdaName.CLOUD_FRONT_SG_GROUP_IP_SYNC.getBucketKey());

        if (theStackAlreadyExists || (theLambdaArtifactExistsInS3)) {
            return true;
        } else {
            logger.error("failed to detect the lambda at {}/{} you must run the lambda publish command first",
                    environmentMetadata.getBucketName(), LambdaName.CLOUD_FRONT_SG_GROUP_IP_SYNC.getBucketKey());
            return false;
        }

    }
}
