package com.nike.cerberus.operation.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nike.cerberus.command.gateway.CreateCloudFrontLogProcessingLambdaConfigCommand;
import com.nike.cerberus.domain.cloudformation.GatewayOutputs;
import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.domain.environment.CloudFrontLogProcessingLambdaConfig;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static com.nike.cerberus.module.CerberusModule.CF_OBJECT_MAPPER;

/**
 * Operation for creating config needed to drive log processing lambda
 */
public class CreateCloudFrontLogProcessingLambdaConfigOperation implements Operation<CreateCloudFrontLogProcessingLambdaConfigCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConfigStore configStore;

    private final CloudFormationService cloudFormationService;

    private final ObjectMapper cloudformationObjectMapper;

    @Inject
    public CreateCloudFrontLogProcessingLambdaConfigOperation(final ConfigStore configStore,
                                                              final CloudFormationService cloudFormationService,
                                                              @Named(CF_OBJECT_MAPPER) final ObjectMapper cloudformationObjectMapper) {
        this.configStore = configStore;
        this.cloudFormationService = cloudFormationService;
        this.cloudformationObjectMapper = cloudformationObjectMapper;
    }

    @Override
    public void run(CreateCloudFrontLogProcessingLambdaConfigCommand command) {
        final String gateWayStackId = configStore.getStackId(StackName.GATEWAY);
        final Map<String, String> stackOutputs = cloudFormationService.getStackOutputs(gateWayStackId);
        final GatewayOutputs gatewayOutputs = cloudformationObjectMapper.convertValue(stackOutputs, GatewayOutputs.class);

        CloudFrontLogProcessingLambdaConfig config = new CloudFrontLogProcessingLambdaConfig();
        config.setManualBlackListIpSetId(gatewayOutputs.getManualBlockIPSetID());
        config.setManualWhiteListIpSetId(gatewayOutputs.getWhiteListIPSetID());
        config.setRateLimitAutoBlackListIpSetId(gatewayOutputs.getAutoBlockIPSetID());
        config.setRequestPerMinuteLimit(command.getRequestPerMinuteLimit());
        config.setRateLimitViolationBlacklistPeriodInMinutes(command.getRateLimitViolationBlacklistPeriodInMinutes());
        config.setSlackWebHookUrl(command.getSlackWebHookUrl());
        config.setSlackIcon(command.getSlackIcon());

        configStore.saveCFLogProcessorLambdaConfig(config);

        logger.info("Config Created and Uploaded to S3");
    }

    @Override
    public boolean isRunnable(CreateCloudFrontLogProcessingLambdaConfigCommand command) {
        return StringUtils.isNotBlank(configStore.getStackId(StackName.GATEWAY));
    }
}
