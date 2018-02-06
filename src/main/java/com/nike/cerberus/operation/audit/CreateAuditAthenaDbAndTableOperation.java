package com.nike.cerberus.operation.audit;

import com.amazonaws.services.athena.AmazonAthenaClient;
import com.amazonaws.services.athena.model.GetQueryExecutionRequest;
import com.amazonaws.services.athena.model.GetQueryResultsRequest;
import com.amazonaws.services.athena.model.GetQueryResultsResult;
import com.amazonaws.services.athena.model.ResultConfiguration;
import com.amazonaws.services.athena.model.StartQueryExecutionRequest;
import com.amazonaws.services.athena.model.StartQueryExecutionResult;
import com.github.tomaslanger.chalk.Chalk;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.audit.CreateAuditAthenaDbAndTableCommand;
import com.nike.cerberus.domain.cloudformation.AuditOutputs;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.AwsClientFactory;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.IOException;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

public class CreateAuditAthenaDbAndTableOperation implements Operation<CreateAuditAthenaDbAndTableCommand> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final CloudFormationService cloudFormationService;
    private final ConfigStore configStore;
    private final AwsClientFactory<AmazonAthenaClient> athenaClientFactory;
    private final String databaseName;
    private final String tableName;
    private final String environmentName;

    @Inject
    public CreateAuditAthenaDbAndTableOperation(CloudFormationService cloudFormationService,
                                                ConfigStore configStore,
                                                @Named(ENV_NAME) String environmentName,
                                                AwsClientFactory<AmazonAthenaClient> athenaClientFactory) {

        this.cloudFormationService = cloudFormationService;
        this.configStore = configStore;
        this.athenaClientFactory = athenaClientFactory;

        databaseName = environmentName + "_audit_db";
        tableName = databaseName + ".audit_data";
        this.environmentName = environmentName;
    }

    @Override
    public void run(CreateAuditAthenaDbAndTableCommand command) {
        AuditOutputs outputs =
                configStore.getStackOutputs(configStore.getPrimaryRegion(),
                        Stack.AUDIT.getFullName(environmentName), AuditOutputs.class);

        String bucketName = outputs.getAuditBucketName();

        log.info("Creating Athena DB");
        String createDb = "CREATE DATABASE IF NOT EXISTS " + databaseName + ";";
        log.info(executeAthenaQuery(createDb, bucketName).toString());
        log.info("Creating table");
        String createAuditTable;
        try {
            String template = "/com/nike/cerberus/operation/audit/create_audit_table.ddl";
            createAuditTable = IOUtils.toString(getClass().getResourceAsStream(template), ConfigConstants.DEFAULT_ENCODING);
            createAuditTable = createAuditTable.replace("@@TABLE_NAME@@", tableName);
            createAuditTable = createAuditTable.replace("@@BUCKET_NAME@@", bucketName);
        } catch (IOException e) {
            throw new RuntimeException("failed to load create athena table template", e);
        }
        log.info(executeAthenaQuery(createAuditTable, bucketName).toString());

        String msg = Chalk.on("ATTENTION: ").red().bold().toString() +
                "Table creation complete, please note that before you execute queries against '" + tableName + "'\n" +
                "You will have to run the following query '" + Chalk.on("MSCK REPAIR TABLE " + tableName).green().bold().toString() + "'\n" +
                "CMS will uploads logs every 5 minutes and creates partition folders for every hour.\n" +
                "You can automate that query to run every hour or run it before you query audit data.\n" +
                "That query is free and scans the S3 folders in the audit bucket and add the new partitions (The hour folders)";

        log.info(msg);
    }

    @Override
    public boolean isRunnable(CreateAuditAthenaDbAndTableCommand command) {
        boolean isRunnable = true;

        if (! cloudFormationService.isStackPresent(configStore.getPrimaryRegion(), Stack.AUDIT.getFullName(environmentName))) {
            log.error("You must create the audit stack using create-audit-logging-stack command");
            isRunnable = false;
        }

        return isRunnable;
    }

    /**
     * Executes an Athena query and waits for it to finish returning the results
     */
    private GetQueryResultsResult executeAthenaQuery(String query, String bucketName) {
        AmazonAthenaClient athena = athenaClientFactory.getClient(configStore.getPrimaryRegion());

        StartQueryExecutionResult result = athena
                .startQueryExecution(new StartQueryExecutionRequest()
                        .withQueryString(query)
                        .withResultConfiguration(new ResultConfiguration().withOutputLocation(String.format("s3://%s/results/", bucketName)))
                );

        String id = result.getQueryExecutionId();

        String state;
        do {
            state = athena.getQueryExecution(new GetQueryExecutionRequest().withQueryExecutionId(id)).getQueryExecution().getStatus().getState();
            log.info("polling for query to finish: current status: {}", state);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.info("Failed to sleep", e);
                Thread.currentThread().interrupt();
            }
        } while (state.equals("RUNNING"));

        log.info("The query: {} is in state: {}, fetching results", id, state);

        return athena.getQueryResults(new GetQueryResultsRequest().withQueryExecutionId(id));
    }
}
