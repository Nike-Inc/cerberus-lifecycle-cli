package com.nike.cerberus.operation.vault;

import com.amazonaws.services.cloudformation.model.StackStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.vault.CreateVaultClusterCommand;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.cloudformation.BaseOutputs;
import com.nike.cerberus.domain.cloudformation.VaultParameters;
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
import java.util.Optional;

import static com.nike.cerberus.module.CerberusModule.CF_OBJECT_MAPPER;

/**
 * Operation to create the Vault cluster.
 */
public class CreateVaultClusterOperation implements Operation<CreateVaultClusterCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final EnvironmentMetadata environmentMetadata;

    private final CloudFormationService cloudFormationService;

    private final Ec2UserDataService ec2UserDataService;

    private final UuidSupplier uuidSupplier;

    private final ConfigStore configStore;

    private final ObjectMapper cloudformationObjectMapper;

    @Inject
    public CreateVaultClusterOperation(final EnvironmentMetadata environmentMetadata,
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
    public void run(final CreateVaultClusterCommand command) {
        final String uniqueStackName = String.format("%s-%s", StackName.VAULT.getName(), uuidSupplier.get());
        final BaseOutputs baseOutputs = configStore.getBaseStackOutputs();
        final Optional<String> vaultServerCertificateArn = configStore.getServerCertificateArn(StackName.VAULT);
        final Optional<String> pubKey = configStore.getCertPart(StackName.VAULT, ConfigConstants.CERT_PART_PUBKEY);
        final String internalElbCname = configStore.getInternalElbCname(StackName.VAULT);

        if (!vaultServerCertificateArn.isPresent() || !pubKey.isPresent()) {
            throw new IllegalStateException("Vault server certificate has not been uploaded!");
        }

        final VaultParameters vaultParameters = new VaultParameters()
                .setInstanceProfileName(baseOutputs.getVaultInstanceProfileName())
                .setVaultClientSgId(baseOutputs.getVaultClientSgId())
                .setVaultServerSgId(baseOutputs.getVaultServerSgId())
                .setVaultServerElbSgId(baseOutputs.getVaultServerElbSgId())
                .setConsulClientSgId(baseOutputs.getConsulClientSgId())
                .setConsulServerSgId(baseOutputs.getConsulServerSgId())
                .setToolsIngressSgId(baseOutputs.getToolsIngressSgId())
                .setVpcId(baseOutputs.getVpcId())
                .setVpcSubnetIdForAz1(baseOutputs.getVpcSubnetIdForAz1())
                .setVpcSubnetIdForAz2(baseOutputs.getVpcSubnetIdForAz2())
                .setVpcSubnetIdForAz3(baseOutputs.getVpcSubnetIdForAz3())
                .setHostedZoneId(baseOutputs.getVpcHostedZoneId())
                .setCname(internalElbCname);

        vaultParameters.getSslConfigParameters().setCertPublicKey(pubKey.get());
        vaultParameters.getSslConfigParameters().setSslCertificateArn(vaultServerCertificateArn.get());

        vaultParameters.getLaunchConfigParameters().setAmiId(command.getStackDelegate().getAmiId());
        vaultParameters.getLaunchConfigParameters().setInstanceSize(command.getStackDelegate().getInstanceSize());
        vaultParameters.getLaunchConfigParameters().setKeyPairName(command.getStackDelegate().getKeyPairName());
        vaultParameters.getLaunchConfigParameters().setUserData(
                ec2UserDataService.getUserData(StackName.VAULT, command.getStackDelegate().getOwnerGroup()));

        vaultParameters.getTagParameters().setTagEmail(command.getStackDelegate().getOwnerEmail());
        vaultParameters.getTagParameters().setTagName(ConfigConstants.ENV_PREFIX + environmentMetadata.getName());
        vaultParameters.getTagParameters().setTagCostcenter(command.getStackDelegate().getCostcenter());

        final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {};
        final Map<String, String> parameters = cloudformationObjectMapper.convertValue(vaultParameters, typeReference);

        final String stackId = cloudFormationService.createStack(cloudFormationService.getEnvStackName(uniqueStackName),
                parameters, ConfigConstants.VAULT_STACK_TEMPLATE_PATH, true);

        final StackStatus endStatus =
                cloudFormationService.waitForStatus(stackId,
                        Sets.newHashSet(StackStatus.CREATE_COMPLETE, StackStatus.ROLLBACK_COMPLETE));

        if (endStatus != StackStatus.CREATE_COMPLETE) {
            final String errorMessage = String.format("Unexpected end status: %s", endStatus.name());
            logger.error(errorMessage);

            throw new UnexpectedCloudFormationStatusException(errorMessage);
        }

        logger.info("Uploading data to the configuration bucket.");
        configStore.storeStackId(StackName.VAULT, stackId);

        logger.info("Uploading complete.");
    }

    @Override
    public boolean isRunnable(final CreateVaultClusterCommand command) {
        boolean isRunnable = true;
        final String consulStackId = configStore.getStackId(StackName.CONSUL);
        final String certificateName = configStore.getServerCertificateName(StackName.VAULT);
        final boolean hasVaultConfig = configStore.hasVaultConfig();

        if (StringUtils.isBlank(consulStackId) || !cloudFormationService.isStackPresent(consulStackId)) {
            logger.error("No Consul stack defined for this environment!");
            isRunnable = false;
        }

        if (StringUtils.isBlank(certificateName)) {
            logger.error("Certificate has not been uploaded for Vault!");
            isRunnable = false;
        }

        if (!hasVaultConfig) {
            logger.error("No configuration for Vault exists for this environment!");
            isRunnable = false;
        }

        return isRunnable;
    }
}
