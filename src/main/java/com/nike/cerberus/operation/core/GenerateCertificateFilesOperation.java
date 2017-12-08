/*
 * Copyright (c) 2017 Nike, Inc.
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

import com.github.tomaslanger.chalk.Chalk;
import com.nike.cerberus.command.core.GenerateCertificateFilesCommand;
import com.nike.cerberus.command.core.GenerateCertificateFilesCommandParametersDelegate;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CertificateService;
import com.nike.cerberus.service.ConsoleService;
import com.nike.cerberus.store.ConfigStore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;
import static com.nike.cerberus.service.ConsoleService.DefaultAction.NO;
import static com.nike.cerberus.service.ConsoleService.DefaultAction.YES;

/**
 * Operation that uses the cert service to generate the certificates needed to enable https in a Cerberus Env.
 */
public class GenerateCertificateFilesOperation implements Operation<GenerateCertificateFilesCommand> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ConfigStore configStore;
    private final CertificateService certService;
    private final String environmentName;
    private final ConsoleService consoleService;

    @Inject
    public GenerateCertificateFilesOperation(ConfigStore configStore,
                                             CertificateService certService,
                                             @Named(ENV_NAME) String environmentName,
                                             ConsoleService consoleService) {

        this.configStore = configStore;
        this.certService = certService;
        this.environmentName = environmentName;
        this.consoleService = consoleService;
    }

    @SuppressFBWarnings(
            value = "REC_CATCH_EXCEPTION",
            justification = "I do not want to catch 1000 individual exceptions"
    )
    @Override
    public void run(GenerateCertificateFilesCommand command) {
        GenerateCertificateFilesCommandParametersDelegate parameters = 
                command.getGenerateCertificateFilesCommandParametersDelegate();
        
        // The common name ex: demo.example.com
        String commonName = StringUtils.isNotBlank(parameters.getEdgeDomainNameOverride()) ?
                parameters.getEdgeDomainNameOverride() :
                String.format("%s.%s", environmentName, parameters.getBaseDomainName());

        // origin name san ex: origin.demo.example.com
        String originName = StringUtils.isNotBlank(parameters.getOriginDomainNameOverride()) ?
                parameters.getOriginDomainNameOverride() :
                String.format("origin.%s.%s", environmentName, parameters.getBaseDomainName());

        // The region specific subject alternate name for the load balancer. EX demo.us-west-2.example.com
        String loadBalancerName = StringUtils.isNotBlank(parameters.getLoadBalancerDomainNameOverride()) ?
                parameters.getLoadBalancerDomainNameOverride() :
                String.format("%s.%s.%s", environmentName, configStore.getPrimaryRegion().getName(),
                        parameters.getBaseDomainName());

        Set<String> subjectAlternativeNames = new HashSet<>();
        subjectAlternativeNames.addAll(parameters.getSubjectAlternativeNames());
        subjectAlternativeNames.add(originName);
        subjectAlternativeNames.add(loadBalancerName);

        // Enable the use of the hard coded lets encrypt cert if enabled
        if (parameters.enableLetsEncryptCertfix()) {
            log.warn("Setting acme4j.le.certfix system property to 'true', this only works if you use the special " +
                    "acme:// lets encrypt addr. See: https://shredzone.org/maven/acme4j/usage/session.html" +
                    " and https://shredzone.org/maven/acme4j/provider.html");
            
            System.setProperty("acme4j.le.certfix", "true");
        }

        try {
            // Use a temp dir or local dir if provided
            File certDir = new File(parameters.getCertDir());

            // check that we can write to the provided dir
            FileUtils.forceMkdir(certDir);
            if (!certDir.isDirectory() || !certDir.canWrite()) {
                throw new RuntimeException("The certificate directory is not a directory or is not writable, path: " 
                        + certDir.getAbsolutePath());
            }

            // confirm with user
            String msg = String.format("Preparing to generate certs in %s with Common Name: %s and Subject Alternative Names: %s",
                    certDir.getAbsolutePath(), commonName, String.join(", ", subjectAlternativeNames));
            if (command.getGenerateCertificateFilesCommandParametersDelegate().isTty()) {
                consoleService.askUserToProceed(msg, NO);
            } else {
                log.info(msg);
            }

            // generate the certs
            certService.generateCerts(
                    certDir,
                    parameters.getAcmeApiUrl(),
                    commonName,
                    subjectAlternativeNames,
                    parameters.getHostedZoneId(),
                    parameters.getContactEmail(),
                    command.getGenerateCertificateFilesCommandParametersDelegate().isAutoAcceptAcmeTos()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate certs", e);
        }
    }

    @Override
    public boolean isRunnable(GenerateCertificateFilesCommand command) {
        boolean isRunnable = true;
        if (command.getGenerateCertificateFilesCommandParametersDelegate().isTty()) {
            boolean filesAlreadyPreset = true;
            File certDir = new File(command.getGenerateCertificateFilesCommandParametersDelegate().getCertDir());
            try {
                certService.checkForRequiredFiles(certDir);
            } catch (RuntimeException e) {
                filesAlreadyPreset = false;
            }
            if (filesAlreadyPreset) {
                try {
                    consoleService.askUserToProceed(
                            Chalk.on(String.format("The required files already exists in %s would you like generate " +
                                    "new files over these?", certDir.getAbsolutePath())).green().toString(), YES);
                } catch (RuntimeException e) {
                    isRunnable = false;
                }
            }
        }
        return isRunnable;
    }
}