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

package com.nike.cerberus.command.core;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.GenerateCertsOperation;

import java.util.ArrayList;
import java.util.List;

import static com.nike.cerberus.command.core.GenerateCertsCommand.COMMAND_DESCRIPTION;
import static com.nike.cerberus.command.core.GenerateCertsCommand.COMMAND_NAME;

@Parameters(
        commandNames = COMMAND_NAME,
        commandDescription = COMMAND_DESCRIPTION
)
public class GenerateCertsCommand implements Command {

    public static final String COMMAND_NAME = "generate-certificates";
    public static final String COMMAND_DESCRIPTION = "Generates the TLS certificates needed to enable https " +
            "through out the system, using an ACME provider such as LetsEncrypt";

    public static final String BASE_DOMAIN_LONG_ARG = "--base-domain";

    public static final String HOSTED_ZONE_ID_LONG_ARG = "--hosted-zone-id";

    public static final String ADDITIONAL_SUBJECT_ALT_NAME_LONG_ARG = "--additional-subject-alternative-name";

    public static final String ENABLE_LE_CERTFIX_LONG_ARG = "--enable-letsecrypt-certfix";

    public static final String CERT_FOLDER_LONG_ARG = "--local-certificate-directory";

    public static final String ACME_API_LONG_ARG = "--acme-api-url";

    public static final String CONTACT_EMAIL_LONG_ARG = "--contact-email";

    public static final String LETS_ENCRYPT_ACME_API_URI = "acme://letsencrypt.org";


    @Parameter(
            names = {
                    BASE_DOMAIN_LONG_ARG
            },
            description = "The base domain for the environment that this command will use to generate the following " +
                    "subject name {env}.{base-domain} and with the following subject alternative name {env}.{region}.{base-domain}\n" +
                    "ex: cerberus -e demo -r us-west-2 generate-certificates --base-domain cerberus.example would make a cert for demo.cerberus.example " +
                    "with a sans of demo.us-west-2.cerberus.example so that we can create a CNAME record for " +
                    "demo.cerberus.example that will point to an ALB in us-west-2 with a CNAME record of " +
                    "demo.us-west-2.cerberus.example and the cert will be valid for both",
            required = true
    )
    private String baseDomainName;

    public String getBaseDomainName() {
        return baseDomainName;
    }

    @Parameter(
            names = {
                    HOSTED_ZONE_ID_LONG_ARG
            },
            description = "The AWS Route 53 hosted zone id that is configured to create records for the base domain",
            required = true
    )
    private String hostedZoneId;

    public String getHostedZoneId() {
        return hostedZoneId;
    }

    @Parameter(
            names = {
                    ADDITIONAL_SUBJECT_ALT_NAME_LONG_ARG
            },
            description = "Alternative subject names, this should be any additional cnames that need to be secured. such as "
    )
    private List<String> subjectAlternativeNames = new ArrayList<>();

    public List<String> getSubjectAlternativeNames() {
        return subjectAlternativeNames;
    }

    @Parameter(
            names = {
                    ENABLE_LE_CERTFIX_LONG_ARG
            },
            description = "This command uses the acme4j client to communicate with the ACME server, " +
                    "it supports uses a hardcoded letsencrypt cert to get around ssl errors, you can use this " +
                    "flag if your truststore is not configured to trust LE certificates, which can be found " +
                    " here: https://letsencrypt.org/certificates/"
    )
    private boolean EnableLetsEncryptCertfix = false;

    public boolean enableLetsEncryptCertfix() {
        return EnableLetsEncryptCertfix;
    }

    @Parameter(
            names = {
                    CERT_FOLDER_LONG_ARG
            },
            description = "A local writable folder to store the generated key and cert files",
            required = true
    )
    private String certDir;

    public String getCertDir() {
        return certDir;
    }

    @Parameter(
            names = {
                    ACME_API_LONG_ARG
            },
            description = "The ACME provider API URL to use, e.g. Let's Encrypt: "  + LETS_ENCRYPT_ACME_API_URI,
            required = true
    )
    private String acmeApiUrl;

    public String getAcmeApiUrl() {
        return acmeApiUrl;
    }

    @Parameter(
            names = {
                    CONTACT_EMAIL_LONG_ARG
            },
            description = "The email contact to use when creating the certificates",
            required = true
    )
    private String contactEmail;

    public String getContactEmail() {
        return contactEmail;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return GenerateCertsOperation.class;
    }
}
