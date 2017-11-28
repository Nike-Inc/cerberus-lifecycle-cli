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

import java.util.ArrayList;
import java.util.List;

public class GenerateCertificateFilesCommandParametersDelegate {
    
    public static final String BASE_DOMAIN_LONG_ARG = "--base-domain";
    public static final String EDGE_DOMAIN_NAME_OVERRIDE_LONG_ARG = "--edge-domain-name-override";
    public static final String ORIGIN_DOMAIN_NAME_OVERRIDE_LONG_ARG = "--origin-domain-name-override";
    public static final String LOAD_BALANCER_DOMAIN_NAME_OVERRIDE_LONG_ARG = "--load-balancer-domain-name-override";
    public static final String HOSTED_ZONE_ID_LONG_ARG = "--hosted-zone-id";
    public static final String SUBJECT_ALT_NAME_LONG_ARG = "--subject-alternative-name";
    public static final String ENABLE_LE_CERTFIX_LONG_ARG = "--enable-letsecrypt-certfix";
    public static final String CERT_FOLDER_LONG_ARG = "--local-certificate-directory";
    public static final String ACME_API_LONG_ARG = "--acme-api-url";
    public static final String CONTACT_EMAIL_LONG_ARG = "--contact-email";
    public static final String NO_TTY_LONG_ARG = "--no-tty";
    public static final String ACCEPT_ACME_TOS = "--no-tty-force-acme-tos-accept";

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
                    "demo.us-west-2.cerberus.example and the cert will be valid for both"
    )
    private String baseDomainName;
    
    @Parameter(
            names = {
                    EDGE_DOMAIN_NAME_OVERRIDE_LONG_ARG
            },
            description = "This command uses {environment}.{base-domain} as the common name, override it with this option"
    )
    private String edgeDomainNameOverride;
    
    @Parameter(
            names = {
                    ORIGIN_DOMAIN_NAME_OVERRIDE_LONG_ARG
            },
            description = "origin domain name defaults to origin.{environment-name}.{base-domain}, " +
                    "this command automatically creates a subject alternate name for this, override it with this option"
    )
    private String originDomainNameOverride;
    
    @Parameter(
            names = {
                    LOAD_BALANCER_DOMAIN_NAME_OVERRIDE_LONG_ARG
            },
            description = "the load balancer domain name defaults to {environment-name}.{primary-primaryRegion}.{base-domain}, " +
                    "this command automatically creates a subject alternate name for this, override it with this option"
    )
    private String loadBalancerDomainNameOverride;
    
    @Parameter(
            names = {
                    HOSTED_ZONE_ID_LONG_ARG
            },
            description = "The AWS Route 53 hosted zone id that is configured to create records for the base domain",
            required = true
    )
    private String hostedZoneId;
    
    @Parameter(
            names = {
                    SUBJECT_ALT_NAME_LONG_ARG
            },
            description = "Alternative subject names, this should be any additional cnames that need to be secured. such as "
    )
    private List<String> subjectAlternativeNames = new ArrayList<String>();

    @Parameter(
            names = {
                    ENABLE_LE_CERTFIX_LONG_ARG
            },
            description = "This command uses the acme4j client to communicate with the ACME server, " +
                    "it supports uses a hardcoded letsencrypt cert to get around ssl errors, you can use this " +
                    "flag if your truststore is not configured to trust LE certificates, which can be found " +
                    " here: https://letsencrypt.org/certificates/"
    )
    private boolean enableLetsEncryptCertfix = false;

    @Parameter(
            names = {
                    CERT_FOLDER_LONG_ARG
            },
            description = "A local writable folder to store the generated key and cert files",
            required = true
    )
    private String certDir;
    
    @Parameter(
            names = {
                    ACME_API_LONG_ARG
            },
            description = "The ACME provider API URL to use, e.g. Let's Encrypt: " + LETS_ENCRYPT_ACME_API_URI,
            required = true
    )
    private String acmeApiUrl;
    
    @Parameter(
            names = {
                    CONTACT_EMAIL_LONG_ARG
            },
            description = "The email contact to use when creating the certificates",
            required = true
    )
    private String contactEmail;

    @Parameter(
            names = {
                    NO_TTY_LONG_ARG
            },
            description = "Flag to supply when running command in an environment without TTY, such as a build job"
    )
    private boolean tty = false;

    @Parameter(
            names = {
                    ACCEPT_ACME_TOS
            },
            description = "If supplying --no-tty and creating a new ACME account you must go read the TOS and supply " +
                    "this flag to indicate that you accept the TOS"
    )
    private boolean autoAcceptAcmeTos = false;

    public String getBaseDomainName() {
        return baseDomainName;
    }

    public String getEdgeDomainNameOverride() {
        return edgeDomainNameOverride;
    }

    public String getOriginDomainNameOverride() {
        return originDomainNameOverride;
    }

    public String getLoadBalancerDomainNameOverride() {
        return loadBalancerDomainNameOverride;
    }

    public String getHostedZoneId() {
        return hostedZoneId;
    }

    public List<String> getSubjectAlternativeNames() {
        return subjectAlternativeNames;
    }

    public boolean enableLetsEncryptCertfix() {
        return enableLetsEncryptCertfix;
    }

    public String getCertDir() {
        return certDir;
    }

    public String getAcmeApiUrl() {
        return acmeApiUrl;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public boolean isTty() {
        return tty;
    }

    public boolean isAutoAcceptAcmeTos() {
        return autoAcceptAcmeTos;
    }
}