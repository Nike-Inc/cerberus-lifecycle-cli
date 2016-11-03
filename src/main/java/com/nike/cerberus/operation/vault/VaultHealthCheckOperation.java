package com.nike.cerberus.operation.vault;

import com.beust.jcommander.internal.Lists;
import com.github.tomaslanger.chalk.Ansi;
import com.github.tomaslanger.chalk.Chalk;
import com.google.inject.Inject;
import com.nike.cerberus.command.vault.VaultHealthCheckCommand;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.vault.VaultAdminClientFactory;
import com.nike.vault.client.VaultAdminClient;
import com.nike.vault.client.model.VaultHealthResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class VaultHealthCheckOperation implements Operation<VaultHealthCheckCommand> {

    private final VaultAdminClientFactory vaultAdminClientFactory;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    public VaultHealthCheckOperation(VaultAdminClientFactory vaultAdminClientFactory) {
        this.vaultAdminClientFactory = vaultAdminClientFactory;
    }

    @Override
    public void run(VaultHealthCheckCommand command) {
        List<String> msgList = Lists.newLinkedList();
        boolean toggle = false;
        do {
            final List<VaultAdminClient> clients = vaultAdminClientFactory.getClientsForCluster();
            clients.forEach(client -> {
                try {
                    VaultHealthResponse response = client.health();
                    msgList.add(String.format("%s: Initialized: %s, Sealed: %s, Standby: %s",
                            client.getVaultUrl(),
                            colorify(response.isInitialized()),
                            colorify(response.isSealed()),
                            colorify(response.isStandby())));
                } catch (Throwable t) {
                    msgList.add(String.format("ERROR: %s", t.getMessage()));
                }
            });

            String sym = toggle ? "*" : "+";
            sym = StringUtils.repeat(sym, 20);

            if (command.isPoll()) {
                logger.info(Ansi.eraseScreen());
            }

            logger.info(String.format("%s - Vault Health Status - %s", sym, sym));
            msgList.forEach(logger::info);
            msgList.clear();
            toggle = !toggle;

            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(1));
            } catch (InterruptedException e) {
                break;
            }
        } while (command.isPoll());
    }



    private String colorify(boolean status) {
        return status ? Chalk.on(Boolean.TRUE.toString()).green().bold().toString() : 
                Chalk.on(Boolean.FALSE.toString()).red().bold().toString();
    }

    @Override
    public boolean isRunnable(VaultHealthCheckCommand command) {
        final boolean hasInstances = vaultAdminClientFactory.hasVaultInstances();

        if (!hasInstances) {
            logger.warn("No Vault instances detected for this environment, exiting...");
        }

        return hasInstances;
    }
}
