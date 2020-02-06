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

package com.nike.cerberus.operation.audit;

import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.audit.UpdateAuditAthenaTableCommand;
import com.nike.cerberus.domain.cloudformation.AuditOutputs;
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

public class UpdateAuditAthenaTableOperation implements Operation<UpdateAuditAthenaTableCommand> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final CloudFormationService cloudFormationService;
    private final ConfigStore configStore;
    private final AthenaService athenaService;
    private final String databaseName;
    private final String tableName;
    private final String environmentName;

    @Inject
    public UpdateAuditAthenaTableOperation(CloudFormationService cloudFormationService,
                                           ConfigStore configStore,
                                           @Named(ENV_NAME) String environmentName,
                                           AthenaService athenaService) {

        this.cloudFormationService = cloudFormationService;
        this.configStore = configStore;
        this.athenaService = athenaService;

        databaseName = environmentName + "_audit_db";
        tableName = databaseName + ".audit_data";
        this.environmentName = environmentName;
    }

    @Override
    public void run(UpdateAuditAthenaTableCommand command) {
        AuditOutputs outputs =
                configStore.getStackOutputs(configStore.getPrimaryRegion(),
                        Stack.AUDIT.getFullName(environmentName), AuditOutputs.class);

        String bucketName = outputs.getAuditBucketName();

        log.info("Dropping table to update");
        String dropTable = "DROP TABLE IF EXISTS " + tableName + ";";
        log.info(athenaService.executeAthenaQuery(dropTable, bucketName, configStore.getPrimaryRegion()).toString());

        log.info("Creating new table with additional columns");
        String updateAuditTable;
        try {
            String template = "/com/nike/cerberus/operation/audit/create_audit_table.ddl";
            updateAuditTable = IOUtils.toString(getClass().getResourceAsStream(template), ConfigConstants.DEFAULT_ENCODING);
            updateAuditTable = updateAuditTable.replace("@@TABLE_NAME@@", tableName);
            updateAuditTable = updateAuditTable.replace("@@BUCKET_NAME@@", bucketName);
        } catch (IOException e) {
            throw new RuntimeException("failed to load update athena table template", e);
        }
        log.info(athenaService.executeAthenaQuery(updateAuditTable, bucketName, configStore.getPrimaryRegion()).toString());

        log.info("Repairing table partitions");
        String repairTablePartitions = "MSCK REPAIR TABLE " + tableName + ";";
        log.info(athenaService.executeAthenaQuery(repairTablePartitions, bucketName, configStore.getPrimaryRegion()).toString());
    }

    @Override
    public boolean isRunnable(UpdateAuditAthenaTableCommand command) {
        boolean isRunnable = true;

        if (! cloudFormationService.isStackPresent(configStore.getPrimaryRegion(), Stack.AUDIT.getFullName(environmentName))) {
            log.error("You must create the audit stack using create-audit-logging-stack command");
            isRunnable = false;
        }
        return isRunnable;
    }
}
