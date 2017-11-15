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

import com.nike.cerberus.command.core.GenerateCertsCommand;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CertificateService;
import com.nike.cerberus.service.ConsoleService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static com.nike.cerberus.service.ConsoleService.DefaultAction.NO;

/**
 * Operation that uses the cert service to generate the certificates needed to enable https in a Cerberus Env.
 */
public class GenerateCertsOperation implements Operation<GenerateCertsCommand> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final CertificateService certService;
    private final EnvironmentMetadata environmentMetadata;
    private final ConsoleService consoleService;

    @Inject
    public GenerateCertsOperation(CertificateService certService,
                                  EnvironmentMetadata environmentMetadata,
                                  ConsoleService consoleService) {

        this.certService = certService;
        this.environmentMetadata = environmentMetadata;
        this.consoleService = consoleService;
    }

    @SuppressFBWarnings(
            value = "REC_CATCH_EXCEPTION",
            justification = "I do not want to catch 1000 individual exceptions"
    )
    @Override
    public void run(GenerateCertsCommand command) {
        // The common name ex: demo.example.com
        String commonName = StringUtils.isNotBlank(command.getEdgeDomainNameOverride()) ?
                command.getEdgeDomainNameOverride() :
                String.format("%s.%s", environmentMetadata.getName(), command.getBaseDomainName());

        // origin name san ex: origin.demo.example.com
        String originName = StringUtils.isNotBlank(command.getOriginDomainNameOverride()) ?
                command.getOriginDomainNameOverride() :
                String.format("origin.%s.%s", environmentMetadata.getName(), command.getBaseDomainName());

        // The region specific subject alternate name for the load balancer. EX demo.us-west-2.example.com
        String loadBalancerName = StringUtils.isNotBlank(command.getLoadBalancerDomainNameOverride()) ?
                command.getLoadBalancerDomainNameOverride() :
                String.format("%s.%s.%s", environmentMetadata.getName(), environmentMetadata.getRegionName(), command.getBaseDomainName());

        Set<String> subjectAlternativeNames = new HashSet<>();
        subjectAlternativeNames.addAll(command.getSubjectAlternativeNames());
        subjectAlternativeNames.add(originName);
        subjectAlternativeNames.add(loadBalancerName);

        // confirm with user
        consoleService.askUserToProceed(String.format("Preparing to generate certs with Common Name: %s and Subject Alternative Names: %s",
                commonName, String.join(", ", subjectAlternativeNames)), NO);

        // Enable the use of the hard coded lets encrypt cert if enabled
        if (command.enableLetsEncryptCertfix()) {
            log.warn("Setting acme4j.le.certfix system property to 'true', this only works if you use the special acme:// lets encrypt addr. See: https://shredzone.org/maven/acme4j/usage/session.html and https://shredzone.org/maven/acme4j/provider.html");
            System.setProperty("acme4j.le.certfix", "true");
        }

        try {
            // Use a temp dir or local dir if provided
            File certDir = new File(command.getCertDir());

            // check that we can write to the provided dir
            FileUtils.forceMkdir(certDir);
            if (!certDir.isDirectory() || !certDir.canWrite()) {
                throw new RuntimeException("The certificate directory is not a directory or is not writable, path: " + certDir.getAbsolutePath());
            }

            // generate the certs
            certService.generateCerts(
                    certDir,
                    command.getAcmeApiUrl(),
                    commonName,
                    subjectAlternativeNames,
                    command.getHostedZoneId(),
                    command.getContactEmail()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate certs", e);
        }
    }

    @Override
    public boolean isRunnable(GenerateCertsCommand command) {
        return true;
    }
}
