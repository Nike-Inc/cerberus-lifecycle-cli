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

package com.nike.cerberus.service;

import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeAction;
import com.amazonaws.services.route53.model.ChangeBatch;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.RRType;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.util.io.pem.PemObject;
import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.Registration;
import org.shredzone.acme4j.RegistrationBuilder;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.challenge.Dns01Challenge;
import org.shredzone.acme4j.exception.AcmeConflictException;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.CertificateUtils;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

import javax.inject.Inject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing Cerificates and calls to the ACME cert provider
 * <p>
 * TODO: Update TOS
 * TODO: Rotate ACME user private key
 * TODO: Support Venafi ACME Impl
 */
@SuppressFBWarnings(value = {
        "DM_DEFAULT_ENCODING",
        "REC_CATCH_EXCEPTION"
})
public class CertificateService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // File name of the PKCS #1 private key pem for the ACME registration, this represents a "User"
    public static final String USER_KEY_FILE = "acme-user-private-key-pkcs1.pem";

    // File name of the PKCS #1 private key pem for the Cerberus domain
    public static final String DOMAIN_PKCS1_KEY_FILE = "domain-private-key-pkcs1.pem";

    // File name of the PKCS #8 private key pem for the Cerberus domain
    public static final String DOMAIN_PKCS8_KEY_FILE = "domain-private-key-pkcs8.pem";

    // File name of pub key pem
    public static final String DOMAIN_PUBLIC_KEY_FILE = "domain-public-key.pem";

    // File name of the certificate signing request
    public static final String DOMAIN_CSR_FILE = "domain.csr";

    // File name of the signed certificate with chain
    public static final String DOMAIN_FULL_CERT_WITH_CHAIN_CRT_FILE = "domain-full-cert-with-chain.crt";

    // File name of the signed certificate
    public static final String DOMAIN_CERT_FILE = "domain-cert.crt";

    // File name of the certificate chain
    public static final String DOMAIN_CERT_CHAIN_FILE = "chain.crt";

    // RSA key size of generated key pairs
    private static final int KEY_SIZE = 2048;

    protected static final String CHALLENGE_ENTRY_TEMPLATE = "_acme-challenge.%s";

    private final ConsoleService console;
    private final AmazonRoute53 route53;

    @Inject
    public CertificateService(ConsoleService console,
                              AmazonRoute53 route53) {

        this.console = console;
        this.route53 = route53;
    }

    /**
     * Finds your {@link Registration} at the ACME server. It will be found by your user's
     * public key. If your key is not known to the server yet, a new registration will be
     * created.
     * <p>
     * This is a simple way of finding your {@link Registration}. A better way is to get
     * the URI of your new registration with {@link Registration#getLocation()} and store
     * it somewhere. If you need to get access to your account later, reconnect to it via
     * {@link Registration#bind(Session, URI)} by using the stored location.
     *
     * @param session {@link Session} to bind with
     * @return {@link Registration} connected to your account
     */
    protected Registration findOrRegisterAccount(Session session, String contactEmail) throws AcmeException, IOException {
        Registration reg;
        try {
            // Try to create a new Registration.
            reg = new RegistrationBuilder().addContact("mailto:" + contactEmail).create(session);
            log.info("Registered a new user, URI: " + reg.getLocation());

            // This is a new account. Let the user accept the Terms of Service.
            // We won't be able to authorize domains until the ToS is accepted.
            URI agreement = reg.getAgreement();

            if (!(agreement == null)) {
                acceptAgreement(reg, agreement);
            }
        } catch (AcmeConflictException ex) {
            // The Key Pair is already registered. getLocation() contains the
            // URL of the existing registration's location. Bind it to the session.
            reg = Registration.bind(session, ex.getLocation());
            log.info("Account already exist, URI: {}, no need to create new account", reg.getLocation());
        }
        return reg;
    }

    /**
     * Prompts a user to read and accept or deny a ACME providers TOS
     *
     * @param reg       The registration object
     * @param agreement The link to the TOS
     */
    protected void acceptAgreement(Registration reg, URI agreement) {
        try {
            log.info("Please download and review the Terms of Service: " + agreement);
            String userResponse = console.readLine("Type \"I Accept\" to accept the Terms of Service, anything else will exit: ");
            if (!StringUtils.equalsIgnoreCase("I Accept", userResponse)) {
                throw new RuntimeException("User did not accept the Terms of Service");
            }
            reg.modify().setAgreement(agreement).commit();
            log.info("Updated user's ToS");
        } catch (AcmeException | IOException e) {
            throw new RuntimeException("Failed to accept ACME TOS", e);
        }
    }

    /**
     * Creates a Txt record in Route53
     *
     * @param name         The record name
     * @param digest       The value for the txt record
     * @param hostedZoneId The hosted zone id to create the record in
     */
    protected void generateChallengeTxtEntry(String name, String digest, String hostedZoneId) {
        ResourceRecordSet recordSet = new ResourceRecordSet()
                .withName(name)
                .withType(RRType.TXT)
                .withTTL(10L)
                .withResourceRecords(
                        new ResourceRecord(String.format("\"%s\"", digest))
                );

        createOrUpdateRecordSets(recordSet, hostedZoneId);
    }

    protected void createOrUpdateRecordSets(ResourceRecordSet recordSet, String hostedZoneId) {
        ChangeBatch changeBatch = new ChangeBatch().withChanges(
                new Change()
                        .withAction(ChangeAction.UPSERT)
                        .withResourceRecordSet(recordSet)
        );

        route53.changeResourceRecordSets(new ChangeResourceRecordSetsRequest()
                .withChangeBatch(changeBatch)
                .withHostedZoneId(hostedZoneId));
    }

    /**
     * Uses DNS to get a txt record
     *
     * @param cname the txt record to loop up
     * @return Value of record if present
     */
    protected Optional<String> getTxtRecordValue(String cname) {
        try {
            Lookup lookup = new Lookup(cname, Type.TXT);
            lookup.setResolver(new SimpleResolver());
            lookup.setCache(null);
            Record[] records = lookup.run();
            return Optional.of(records[0].rdataToString());
        } catch (Exception e) {
            log.error("Failed to look up txt record for {} reason: {}", cname, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Loads a key pair from specified file. If the file does not exist,
     * a new key pair is generated and saved.
     *
     * @return {@link KeyPair}.
     */
    protected KeyPair loadOrCreateKeyPair(File file) throws IOException {
        if (file.exists()) {
            try (FileReader fr = new FileReader(file)) {
                return KeyPairUtils.readKeyPair(fr);
            }
        } else {
            KeyPair domainKeyPair = KeyPairUtils.createKeyPair(KEY_SIZE);
            try (FileWriter fw = new FileWriter(file)) {
                KeyPairUtils.writeKeyPair(domainKeyPair, fw);
            }
            return domainKeyPair;
        }
    }


    /**
     * Generates the certs needed for a Cerberus Environment
     *
     * @param certDir                 The folder to store the generated files
     * @param commonName              The primary CNAME for the cert
     * @param subjectAlternativeNames Additional CNAMEs for the cert
     * @param hostedZoneId            The hosted zone id that can be used to create txt records for the ACME ownership challenges
     */
    public void generateCerts(File certDir,
                              String acmeServerUrl,
                              String commonName,
                              Set<String> subjectAlternativeNames,
                              String hostedZoneId,
                              String contactEmail) {

        try {
            List<String> names = new LinkedList<>();
            names.add(commonName); // the first entry counts as the common name
            names.addAll(subjectAlternativeNames);

            KeyPair userKeyPair = loadOrCreateKeyPair(new File(certDir.getAbsolutePath() + File.separator + USER_KEY_FILE));

            Session session = new Session(acmeServerUrl, userKeyPair);
            Registration registration = findOrRegisterAccount(session, contactEmail);

            for (String name : names) {
                Authorization authorization = registration.authorizeDomain(name);
                getChallenges(authorization).forEach(challenge -> doChallenge(challenge, name, hostedZoneId));
            }

            KeyPair domainKeyPair = loadOrCreateKeyPair(new File(certDir.getAbsolutePath() + File.separator + DOMAIN_PKCS1_KEY_FILE));
            createPKCS8PrivateKeyPemFileFromKeyPair(domainKeyPair, certDir);
            createPKCS1PublicKeyPem(domainKeyPair, certDir);
            CSRBuilder csrb = new CSRBuilder();
            csrb.addDomains(names);
            csrb.sign(domainKeyPair);

            try (Writer csrWriter = new FileWriter(new File(certDir.getAbsolutePath() + File.separator + DOMAIN_CSR_FILE))) {
                csrb.write(csrWriter);
            }

            // Now request a signed certificate.
            Certificate certificate = registration.requestCertificate(csrb.getEncoded());

            log.info("Success! The certificate has been generated!");
            log.info("Certificate URI: " + certificate.getLocation());

            // Download the leaf certificate and certificate chain.
            X509Certificate cert = certificate.download();
            X509Certificate[] chain = certificate.downloadChain();

            // Write a combined file containing the certificate and chain.
            try (FileWriter fullCertWriter = new FileWriter(new File(certDir.getAbsolutePath() + File.separator + DOMAIN_FULL_CERT_WITH_CHAIN_CRT_FILE))) {
                CertificateUtils.writeX509CertificateChain(fullCertWriter, cert, chain);
            }
            // write just the cert
            try (FileWriter certWriter = new FileWriter(new File(certDir.getAbsolutePath() + File.separator + DOMAIN_CERT_FILE))) {
                CertificateUtils.writeX509Certificate(cert, certWriter);
            }
            // write just the chain
            try (FileWriter chainWriter = new FileWriter(new File(certDir.getAbsolutePath() + File.separator + DOMAIN_CERT_CHAIN_FILE))) {
                CertificateUtils.writeX509CertificateChain(chainWriter, null, chain);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate certs", e);
        }
    }

    /**
     * Takes a challenge and performs it
     *
     * @param challenge  The challenge
     * @param domainName The domain that is being verified
     * @param hostedZone The hosted zone id if applicable
     */
    protected void doChallenge(Challenge challenge, String domainName, String hostedZone) {
        switch (challenge.getType()) {
            case Dns01Challenge.TYPE:
                doDns01Challenge((Dns01Challenge) challenge, domainName, hostedZone);
                break;
            default:
                throw new RuntimeException("Unsupported challenge type: " + challenge.getType());
        }
    }

    /**
     * Attempts to get an acceptable challenge from the ACME provider.
     * <p>
     * This CLI currently only supports DNS-01 Challenges
     *
     * @param authorization The authorization object from the ACME provider
     * @return a Collection of challenges to complete.
     */
    protected Collection<Challenge> getChallenges(Authorization authorization) {
        List<String[]> desiredChallengeCombos = ImmutableList.of(
                new String[]{Dns01Challenge.TYPE}
        );

        for (String[] desiredChallengeCombo : desiredChallengeCombos) {
            Collection<Challenge> challenges = authorization.findCombination(desiredChallengeCombo);
            if (!challenges.isEmpty()) {
                return challenges;
            }
        }

        String apiSupportedChallenges = new Gson().toJson(authorization.getCombinations());
        String cliSupportedMethods = new Gson().toJson(desiredChallengeCombos);
        throw new RuntimeException("Failed to find a supportable combination of domain verification challenges. " +
                "Supported challenge combos by the API: " + apiSupportedChallenges +
                ", challenge combos supported by the CLI: " + cliSupportedMethods);
    }

    /**
     * Performs the ACME DNS 01 Challenge by creating txt records in Route 53
     *
     * @param challenge    The ACME challenge info with digest
     * @param domainName   The domain name that is being verified
     * @param hostedZoneId The hosted zone id that has permissions to create dns records for the domain name
     */
    protected void doDns01Challenge(Dns01Challenge challenge, String domainName, String hostedZoneId) {
        String digest = challenge.getDigest();
        try {
            String cname = String.format(CHALLENGE_ENTRY_TEMPLATE, domainName);
            generateChallengeTxtEntry(cname, digest, hostedZoneId);
            Optional<String> recordValue;
            do {
                log.info("Waiting for name: '{}' to have txt record digest value: '{}' before triggering challenge", cname, digest);
                Thread.sleep(TimeUnit.SECONDS.toMillis(60));
                recordValue = getTxtRecordValue(cname);
            } while (!recordValue.isPresent() || !recordValue.get().equals(String.format("\"%s\"", digest)));
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to complete DNS 01 challenge", e);
        }

        triggerChallenge(challenge);
    }

    /**
     * Triggers the ACME challenge polling for its status to be complete
     *
     * @param challenge The challenge that is ready to be triggered
     */
    protected void triggerChallenge(Challenge challenge) {
        try {
            challenge.trigger();

            // TODO better polling is possible, this can cause infinite loops
            while (challenge.getStatus() != Status.VALID) {
                log.info("Waiting for challenge to complete");
                Thread.sleep(3000);
                challenge.update();
            }
            log.info("challenge accepted");
        } catch (AcmeException | InterruptedException e) {
            log.error("failed to trigger and wait for challenge to be accepted", e);
        }
    }

    protected void createPKCS1PublicKeyPem(KeyPair keyPair, File certDir) {
        try {
            FileWriter pubKeyWriter = new FileWriter(new File(certDir.getAbsolutePath() + File.separator + DOMAIN_PUBLIC_KEY_FILE));
            try (JcaPEMWriter jw = new JcaPEMWriter(pubKeyWriter)) {
                jw.writeObject(keyPair.getPublic());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create pub key pem", e);
        }
    }

    /**
     * Netty, which is what CMS is using requires the private key to be in PKCS8 format
     * http://netty.io/wiki/sslcontextbuilder-and-private-key.html
     */
    protected void createPKCS8PrivateKeyPemFileFromKeyPair(KeyPair keyPair, File certDir) {
        try {
            JcaPKCS8Generator pkcs8Generator = new JcaPKCS8Generator(keyPair.getPrivate(), null);
            PemObject pemObject = pkcs8Generator.generate();
            FileWriter fileWriter = new FileWriter(new File(certDir.getAbsolutePath()
                    + File.separator + DOMAIN_PKCS8_KEY_FILE));
            try (JcaPEMWriter pemWriter = new JcaPEMWriter(fileWriter)) {
                pemWriter.writeObject(pemObject);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create PKCS8 private key pem", e);
        }
    }
}
