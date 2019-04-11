package com.nike.cerberus.operation.core;

import com.amazonaws.regions.Regions;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.core.CreateAlbLogAthenaDbAndTableCommand;
import com.nike.cerberus.domain.cloudformation.LoadBalancerOutputs;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.AthenaService;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

public class CreateAlbLogAthenaDbAndTableOperation implements Operation<CreateAlbLogAthenaDbAndTableCommand> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final CloudFormationService cloudFormationService;
    private final ConfigStore configStore;
    private final String databaseName;
    private final String tableName;
    private final String environmentName;
    private final AthenaService athenaService;

    @Inject
    public CreateAlbLogAthenaDbAndTableOperation(CloudFormationService cloudFormationService,
                                                 ConfigStore configStore,
                                                 @Named(ENV_NAME) String environmentName,
                                                 AthenaService athenaService) {

        this.cloudFormationService = cloudFormationService;
        this.configStore = configStore;

        databaseName = environmentName + "_alb_db";
        tableName = databaseName + ".alb_logs";
        this.environmentName = environmentName;
        this.athenaService = athenaService;
    }

    @Override
    public void run(CreateAlbLogAthenaDbAndTableCommand command) {
        Regions region = command.getStackRegion().orElse(configStore.getPrimaryRegion());

        LoadBalancerOutputs outputs =
                configStore.getStackOutputs(region,
                        Stack.LOAD_BALANCER.getFullName(environmentName), LoadBalancerOutputs.class);

        String bucketName = outputs.getLoadBalancerAccessLogBucket();
        String accountId = outputs.getLoadBalancerPhysicalId().split(":")[4];

        log.info("Creating Athena DB");
        String createDb = "CREATE DATABASE IF NOT EXISTS " + databaseName + ";";
        log.info(athenaService.executeAthenaQuery(createDb, bucketName, region).toString());
        log.info("Creating table");
        String createAuditTable;
        try {
            String template = "/com/nike/cerberus/operation/log_process/create_alb_log_table.ddl";
            createAuditTable = IOUtils.toString(getClass().getResourceAsStream(template), ConfigConstants.DEFAULT_ENCODING);
            createAuditTable = createAuditTable.replace("@@TABLE_NAME@@", tableName);
            createAuditTable = createAuditTable.replace("@@BUCKET_NAME@@", bucketName);
            createAuditTable = createAuditTable.replace("@@ACCOUNT_ID@@", accountId);
        } catch (IOException e) {
            throw new RuntimeException("failed to load create athena table template", e);
        }
        log.info(athenaService.executeAthenaQuery(createAuditTable, bucketName, region).toString());
    }

    @Override
    public boolean isRunnable(CreateAlbLogAthenaDbAndTableCommand command) {
        Regions region = command.getStackRegion().orElse(configStore.getPrimaryRegion());
        boolean isRunnable = true;

        if (! cloudFormationService.isStackPresent(region, Stack.LOAD_BALANCER.getFullName(environmentName))) {
            log.error("You must create the audit stack using create-alb-log-athena-db-and-table command");
            isRunnable = false;
        }

        return isRunnable;
    }
}
