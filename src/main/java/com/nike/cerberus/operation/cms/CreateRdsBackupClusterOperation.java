package com.nike.cerberus.operation.cms;

import com.amazonaws.services.cloudformation.model.StackStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.cms.CreateRdsBackupClusterCommand;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.cloudformation.BaseOutputs;
import com.nike.cerberus.domain.cloudformation.RdsBackupParameters;
import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.UnexpectedCloudFormationStatusException;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.Ec2UserDataService;
import com.nike.cerberus.store.ConfigStore;
import com.nike.cerberus.util.UuidSupplier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static com.nike.cerberus.module.CerberusModule.CF_OBJECT_MAPPER;

/**
 * Creates the RDS backup cluster.
 */
public class CreateRdsBackupClusterOperation implements Operation<CreateRdsBackupClusterCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final EnvironmentMetadata environmentMetadata;

    private final CloudFormationService cloudFormationService;

    private final Ec2UserDataService ec2UserDataService;

    private final UuidSupplier uuidSupplier;

    private final ConfigStore configStore;

    private final ObjectMapper cloudformationObjectMapper;

    @Inject
    public CreateRdsBackupClusterOperation(final EnvironmentMetadata environmentMetadata,
                                     final CloudFormationService cloudFormationService,
                                     final Ec2UserDataService ec2UserDataService,
                                     final UuidSupplier uuidSupplier,
                                     final ConfigStore configStore,
                                     @Named(CF_OBJECT_MAPPER) final ObjectMapper cloudformationObjectMapper) {
        this.environmentMetadata = environmentMetadata;
        this.cloudFormationService = cloudFormationService;
        this.ec2UserDataService = ec2UserDataService;
        this.uuidSupplier = uuidSupplier;
        this.configStore = configStore;
        this.cloudformationObjectMapper = cloudformationObjectMapper;
    }

    @Override
    public void run(final CreateRdsBackupClusterCommand command) {
        final String uniqueStackName = String.format("%s-%s", StackName.RDSBACKUP.getName(), uuidSupplier.get());
        final BaseOutputs baseOutputs = configStore.getBaseStackOutputs();

        final RdsBackupParameters rdsBackupParameters = new RdsBackupParameters()
                .setInstanceProfileName(baseOutputs.getCmsInstanceProfileName())
                .setCmsSgId(baseOutputs.getCmsSgId())
                .setToolsIngressSgId(baseOutputs.getToolsIngressSgId())
                .setVpcId(baseOutputs.getVpcId())
                .setVpcSubnetIdForAz1(baseOutputs.getVpcSubnetIdForAz1())
                .setVpcSubnetIdForAz2(baseOutputs.getVpcSubnetIdForAz2())
                .setVpcSubnetIdForAz3(baseOutputs.getVpcSubnetIdForAz3());

        rdsBackupParameters.getLaunchConfigParameters().setAmiId(command.getStackDelegate().getAmiId());
        rdsBackupParameters.getLaunchConfigParameters().setInstanceSize(command.getStackDelegate().getInstanceSize());
        rdsBackupParameters.getLaunchConfigParameters().setKeyPairName(command.getStackDelegate().getKeyPairName());
        rdsBackupParameters.getLaunchConfigParameters().setUserData(
                ec2UserDataService.getUserData(StackName.RDSBACKUP, command.getStackDelegate().getOwnerGroup()));

        rdsBackupParameters.getTagParameters().setTagEmail(command.getStackDelegate().getOwnerEmail());
        rdsBackupParameters.getTagParameters().setTagName(ConfigConstants.ENV_PREFIX + environmentMetadata.getName());
        rdsBackupParameters.getTagParameters().setTagCostcenter(command.getStackDelegate().getCostcenter());

        final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {};
        final Map<String, String> parameters = cloudformationObjectMapper.convertValue(rdsBackupParameters, typeReference);

        final String stackId = cloudFormationService.createStack(cloudFormationService.getEnvStackName(uniqueStackName),
                parameters, ConfigConstants.RDSBACKUP_STACK_TEMPLATE_PATH, true);

        logger.info("Uploading data to the configuration bucket.");
        configStore.storeStackId(StackName.RDSBACKUP, stackId);

        final StackStatus endStatus =
                cloudFormationService.waitForStatus(stackId,
                        Sets.newHashSet(StackStatus.CREATE_COMPLETE, StackStatus.ROLLBACK_COMPLETE));

        if (endStatus != StackStatus.CREATE_COMPLETE) {
            final String errorMessage = String.format("Unexpected end status: %s", endStatus.name());
            logger.error(errorMessage);

            throw new UnexpectedCloudFormationStatusException(errorMessage);
        }
    }

    @Override
    public boolean isRunnable(final CreateRdsBackupClusterCommand command) {
        final String cmsStackId = configStore.getStackId(StackName.CMS);

        if (StringUtils.isBlank(cmsStackId) || !cloudFormationService.isStackPresent(cmsStackId)) {
            logger.error("No CMS stack in this environment!");
            return false;
        }

        return true;
    }
}
