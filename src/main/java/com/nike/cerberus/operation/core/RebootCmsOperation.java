/*
 * Copyright (c) 2019 Nike, Inc.
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

package com.nike.cerberus.operation.core;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.github.tomaslanger.chalk.Chalk;
import com.google.inject.Inject;
import com.nike.cerberus.client.HttpClientFactory;
import com.nike.cerberus.command.core.RebootCmsCommand;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.AutoScalingService;
import com.nike.cerberus.service.AwsClientFactory;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.Ec2Service;
import com.nike.cerberus.store.ConfigStore;
import com.nike.vault.client.http.HttpStatus;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;
import static com.nike.cerberus.service.CloudFormationService.MIN_INSTANCES_STACK_PARAMETER_KEY;
import static com.nike.cerberus.service.Ec2Service.EC2_ASG_GROUP_NAME_TAG_KEY;
import static com.nike.cerberus.service.Ec2Service.INSTANCE_STATE_FILTER_NAME;
import static com.nike.cerberus.service.Ec2Service.INSTANCE_STATE_RUNNING_FILTER_VALUE;

/**
 * Reboots all EC2 instances in the given cluster.
 */
public class RebootCmsOperation implements Operation<RebootCmsCommand> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final static String CMS_HEALTH_CHECK_URL = "https://%s:%s/healthcheck";

    private final static Integer CMS_HEALTH_CHECK_PORT = 8443;

    private final static int NUM_SECS_BETWEEN_HEALTH_CHECKS = 5;

    private final static int EXPECTED_NUM_SUCCESSES_AFTER_REBOOT = 10;

    private final static int EXPECTED_NUM_FAILURES_AFTER_REBOOT = 3;

    private final static int EXPECTED_NUM_SUCCESSES_BEFORE_REBOOT = 1;

    private final static int HEALTH_CHECK_FAILED_CODE = -1;

    private final ConfigStore configStore;

    private final CloudFormationService cloudFormationService;

    private final Ec2Service ec2Service;

    private final AutoScalingService autoScalingService;

    private final String environmentName;

    private final AmazonEC2 ec2Client;

    private final HttpClientFactory httpClientFactory;

    @Inject
    public RebootCmsOperation(ConfigStore configStore,
                              CloudFormationService cloudFormationService,
                              Ec2Service ec2Service,
                              AutoScalingService autoScalingService,
                              @Named(ENV_NAME) String environmentName,
                              AwsClientFactory<AmazonEC2Client> amazonS3ClientFactory,
                              HttpClientFactory httpClientFactory) {

        this.configStore = configStore;
        this.cloudFormationService = cloudFormationService;
        this.ec2Service = ec2Service;
        this.autoScalingService = autoScalingService;
        this.environmentName = environmentName;
        this.ec2Client = amazonS3ClientFactory.getClient(configStore.getPrimaryRegion());
        this.httpClientFactory = httpClientFactory;
    }

    @Override
    public void run(final RebootCmsCommand command) {
        log.warn(Chalk.on(
                "If this command fails: the minimum instance size may need to be increased and an EC2 instance" +
                        " may need to be set to 'in-service' state on the auto scaling group").yellow().toString());

        log.warn(Chalk.on("This command will lookup the current public ip of the system it is running on temporary add it to the " +
                "vpc whitelist security group if it is not already on the list. If this command fails you may need to " +
                "ensure that the white list security group get cleaned via the whitelist-cidr-for-vpc-access command").red().toString());

        // white list the current public ip if needed
        Optional<String> tempCidrToWhitelist = getCidrIfWhitelistNeeded();
        tempCidrToWhitelist.ifPresent(this::whitelistCurrentIpCidr);

        try {
            final Stack stack = Stack.CMS;
            final String stackId = stack.getFullName(environmentName);
            final Map<String, String> stackOutputs = cloudFormationService.getStackOutputs(configStore.getPrimaryRegion(), stackId);

            final Map<String, String> stackParameters =
                    cloudFormationService.getStackParameters(configStore.getPrimaryRegion(), stackId);
            final int minInstances = Integer.parseInt(stackParameters.get(MIN_INSTANCES_STACK_PARAMETER_KEY));

            final String autoScalingGroupId = stackOutputs.get(CloudFormationService.AUTO_SCALING_GROUP_LOGICAL_ID_OUTPUT_KEY);
            log.debug("Found auto scaling group id for stack: {}", stackId);

            final Filter isRunningFilter = new Filter(INSTANCE_STATE_FILTER_NAME).withValues(INSTANCE_STATE_RUNNING_FILTER_VALUE);
            final List<Instance> instances = ec2Service.getInstancesByTag(EC2_ASG_GROUP_NAME_TAG_KEY, autoScalingGroupId, isRunningFilter);
            log.debug("Found {} instances by tag: '{}:{}'", instances.size(), EC2_ASG_GROUP_NAME_TAG_KEY, autoScalingGroupId);

            log.info("Temporarily decreasing min instances for ASG: {}", autoScalingGroupId);
            autoScalingService.updateMinInstancesForAutoScalingGroup(autoScalingGroupId, minInstances - 1);

            instances.forEach(instance -> rebootInstance(autoScalingGroupId, instance));

            log.info("Increasing min instances for ASG: {}", autoScalingGroupId);
            autoScalingService.updateMinInstancesForAutoScalingGroup(autoScalingGroupId, minInstances);
        } catch (Throwable e) {
            throw new RuntimeException("failed to do rolling reboot, will attempt to remove current ip from whitelist if needed", e);
        } finally {
            // if we whitelisted the current ip then remove it since we are done
            tempCidrToWhitelist.ifPresent(this::removeCurrentIpCidrFromWhitelist);
        }
    }

    /**
     * Reboot an instance and make sure it comes back healthy
     */
    private void rebootInstance(String autoScalingGroupId, Instance instance) {

        final String healthCheckUrl = String.format(CMS_HEALTH_CHECK_URL, instance.getPublicIpAddress(), CMS_HEALTH_CHECK_PORT);

        log.info("Checking that instance health check is reachable...");
        waitForHealthCheckStatusCode(healthCheckUrl, HttpStatus.OK, EXPECTED_NUM_SUCCESSES_BEFORE_REBOOT);

        final String instanceId = instance.getInstanceId();
        log.info("Setting instance state to standby: {}", instanceId);
        autoScalingService.setInstanceStateToStandby(autoScalingGroupId, instanceId);

        log.info("Rebooting instance: {}", instanceId);
        ec2Service.rebootEc2Instance(instanceId);

        // wait for health check fail to confirm box reboot
        log.info("Waiting for health check failure to confirm reboot...");
        waitForHealthCheckStatusCode(healthCheckUrl, HEALTH_CHECK_FAILED_CODE, EXPECTED_NUM_FAILURES_AFTER_REBOOT);

        log.info("Waiting for health check to pass again to confirm instance is healthy...");
        waitForHealthCheckStatusCode(healthCheckUrl, HttpStatus.OK, EXPECTED_NUM_SUCCESSES_AFTER_REBOOT);

        log.info("Setting instance state to in-service: {}", instanceId);
        autoScalingService.setInstanceStateToInService(autoScalingGroupId, instanceId);
    }

    /**
     * Poll the health check 'n' times, looking for the given response
     *
     * @param healthCheckUrl                  - The health check URL
     * @param numConsecutiveResponsesExpected - The number of times to poll health check
     */
    private void waitForHealthCheckStatusCode(final String healthCheckUrl,
                                              final long expectedStatusCode,
                                              final int numConsecutiveResponsesExpected) {

        int responseCode;
        int consecutiveResponses = 0;

        OkHttpClient healthCheckClient = httpClientFactory.getGenericClientWithCustomTruststore();

        while (consecutiveResponses < numConsecutiveResponsesExpected) {

            responseCode = executeHealthCheck(healthCheckUrl, healthCheckClient);

            if (responseCode == expectedStatusCode) {
                consecutiveResponses++;
            } else if (consecutiveResponses > 0) {
                final String message = Chalk.on("Instance health check did not repeat response code ({}), {} times").red().bold().toString();
                log.debug(message, expectedStatusCode, numConsecutiveResponsesExpected);
                consecutiveResponses = 0;
            }

            try {
                TimeUnit.SECONDS.sleep(NUM_SECS_BETWEEN_HEALTH_CHECKS);
            } catch (InterruptedException ie) {
                log.error(Chalk.on("Timeout between health checks has been interrupted").red().bold().toString());
                return;
            }
        }
    }

    /**
     * Execute the given health check
     *
     * @param healthCheckUrl - Name of that EC2 instance belongs to
     * @return - Response code of the health check
     */
    private int executeHealthCheck(String healthCheckUrl, OkHttpClient okHttpClient) {

        final Request request = new Request.Builder()
                .url(healthCheckUrl)
                .get()
                .build();

        log.debug("Performing to the following request: {}", request);

        final Call healthCheckCall = okHttpClient.newCall(request);

        try {
            final Response response = healthCheckCall.execute();
            log.debug("Health check returned status: {}, URL: {}", response.code(), healthCheckUrl);
            return response.code();
        } catch (Exception e) {
            if (StringUtils.contains(e.getMessage(), "PKIX path building failed")) {
                throw new RuntimeException("Failed to validate certificate, this shouldn't happen because we use a" +
                        " custom trust store for this call using the ca chain you uploaded, so you ca chain might be malformed");
            }
            final String message = Chalk.on("Health check failed, Cause: \"{}\", URL: {}").red().toString();
            log.debug(message, e.getMessage(), healthCheckUrl);
        }

        return HEALTH_CHECK_FAILED_CODE;
    }

    private String getCurrentPublicIpAddress() {
        String whatIsMyIp = "http://checkip.amazonaws.com";
        try {
            Response response = httpClientFactory.getGenericClient()
                    .newCall(new Request.Builder().url(whatIsMyIp).get().build()).execute();
            return StringUtils.trim(response.body().string());
        } catch (IOException e) {
            throw new RuntimeException("Failed to lookup current ip", e);
        }
    }

    @Override
    public boolean isRunnable(final RebootCmsCommand command) {
        final Stack stack = Stack.CMS;
        final String stackId = stack.getFullName(environmentName);
        final Map<String, String> stackParameters =
                cloudFormationService.getStackParameters(configStore.getPrimaryRegion(), stackId);

        if (!stackParameters.containsKey(MIN_INSTANCES_STACK_PARAMETER_KEY)) {
            log.error("Could not find parameter 'minInstances' on stack: {}", stackId);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Looks up the current ip converts it to a CIDR and checks the vpc whitelist sg created by the whitelist vpc for ingress command
     * to see if the current ip is already whitelisted to have access to the CMS Health Check Port.
     *
     * @return an optional of a the current public ip in CIDR form if it needs to be whitelisted.
     */
    private Optional<String> getCidrIfWhitelistNeeded() {
        String currentIp = getCurrentPublicIpAddress();

        String vpcIngressWhitelistSecurityGroup = configStore.getSecurityGroupStackOutputs().getWhitelistIngressSgId();

        DescribeSecurityGroupsResult securityGroupsResult = ec2Client.describeSecurityGroups(
                new DescribeSecurityGroupsRequest().withGroupIds(vpcIngressWhitelistSecurityGroup));

        for (SecurityGroup securityGroup : securityGroupsResult.getSecurityGroups()) {
            for (IpPermission ipPermission : securityGroup.getIpPermissions()) {
                for (IpRange ipRange : ipPermission.getIpv4Ranges()) {
                    SubnetUtils subnetUtils = new SubnetUtils(ipRange.getCidrIp());
                    if (isIpInCidrRange(currentIp, subnetUtils) &&
                            Objects.equals(ipPermission.getFromPort(), CMS_HEALTH_CHECK_PORT) &&
                            Objects.equals(ipPermission.getToPort(), CMS_HEALTH_CHECK_PORT)) {

                        log.info("detected that ip: '{}' already has permission to ingress on port: {}, no need to whitelist",
                                currentIp, CMS_HEALTH_CHECK_PORT);
                        return Optional.empty();
                    }
                }
            }
        }
        return Optional.of(currentIp + "/32");
    }

    private boolean isIpInCidrRange(String ip, SubnetUtils subnetUtils) {
        String cidr = subnetUtils.getInfo().getCidrSignature();
        if (cidr.endsWith("/32") && StringUtils.removeEnd(cidr, "/32").equals(ip)) {
            return true;
        }

        return subnetUtils.getInfo().isInRange(ip);
    }

    private void whitelistCurrentIpCidr(String cidr) {
        log.info("Adding new ip permission to the vpc ingress sg, IP: '{}' From and To Port: {}", cidr, CMS_HEALTH_CHECK_PORT);

        String vpcIngressWhitelistSecurityGroup = configStore.getSecurityGroupStackOutputs().getWhitelistIngressSgId();
        AuthorizeSecurityGroupIngressRequest ingressRequest = new AuthorizeSecurityGroupIngressRequest()
                .withGroupId(vpcIngressWhitelistSecurityGroup)
                .withIpPermissions(getIpPermissionForCidr(cidr));
        ec2Client.authorizeSecurityGroupIngress(ingressRequest);

        try {
            log.info("Sleeping for 1 minute to let sg changes be eventually consistent");
            Thread.sleep(TimeUnit.MINUTES.toMillis(1));
        } catch (InterruptedException e) {
            log.error("Failed to wait for sg to be eventually consistent");
        }
    }

    private void removeCurrentIpCidrFromWhitelist(String cidr) {
        log.info("Removing new ip permission to the vpc ingress sg, IP: '{}' From and To Port: {}", cidr, CMS_HEALTH_CHECK_PORT);

        String vpcIngressWhitelistSecurityGroup = configStore.getSecurityGroupStackOutputs().getWhitelistIngressSgId();
        RevokeSecurityGroupIngressRequest revokeIngressRequest = new RevokeSecurityGroupIngressRequest()
                .withGroupId(vpcIngressWhitelistSecurityGroup)
                .withIpPermissions(getIpPermissionForCidr(cidr));
        ec2Client.revokeSecurityGroupIngress(revokeIngressRequest);
    }

    private IpPermission getIpPermissionForCidr(String cidr) {
        return new IpPermission()
                .withIpv4Ranges(new IpRange().withCidrIp(cidr))
                .withIpProtocol("tcp")
                .withFromPort(CMS_HEALTH_CHECK_PORT)
                .withToPort(CMS_HEALTH_CHECK_PORT);
    }

}
