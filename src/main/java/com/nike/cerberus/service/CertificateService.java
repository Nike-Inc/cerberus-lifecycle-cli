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
import com.amazonaws.util.StringInputStream;
import com.beust.jcommander.internal.Sets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.environment.CertificateInformation;
import com.nike.cerberus.store.ConfigStore;
import com.nike.cerberus.util.UuidSupplier;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.util.io.pem.PemObject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
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
        "REC_CATCH_EXCEPTION",
        "UC_USELESS_OBJECT"
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

    protected static final Set<String> EXPECTED_FILE_NAMES = ImmutableSet.of(
            DOMAIN_CERT_CHAIN_FILE,
            DOMAIN_CERT_FILE,
            DOMAIN_PKCS1_KEY_FILE,
            DOMAIN_PKCS8_KEY_FILE,
            DOMAIN_PUBLIC_KEY_FILE
    );

    private final ConsoleService console;
    private final AmazonRoute53 route53;
    private final UuidSupplier uuidSupplier;
    private final ConfigStore configStore;
    private final IdentityManagementService identityManagementService;
    private final EnvironmentMetadata environmentMetadata;

    @Inject
    public CertificateService(ConsoleService console,
                              AmazonRoute53 route53,
                              UuidSupplier uuidSupplier,
                              ConfigStore configStore,
                              IdentityManagementService identityManagementService,
                              EnvironmentMetadata environmentMetadata) {

        this.console = console;
        this.route53 = route53;
        this.uuidSupplier = uuidSupplier;
        this.configStore = configStore;
        this.identityManagementService = identityManagementService;
        this.environmentMetadata = environmentMetadata;
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

    protected void executeRecordSetChanges(String name, String digest, String hostedZoneId, ChangeAction action) {
        ResourceRecordSet recordSet = new ResourceRecordSet()
                .withName(name)
                .withType(RRType.TXT)
                .withTTL(10L)
                .withResourceRecords(
                        new ResourceRecord(String.format("\"%s\"", digest))
                );

        ChangeBatch changeBatch = new ChangeBatch().withChanges(
                new Change()
                        .withAction(action)
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

            Map<String, Collection<Challenge>> domainChallengeCollectionMap = new HashMap<>();
            for (String name : names) {
                Authorization authorization = registration.authorizeDomain(name);
                domainChallengeCollectionMap.put(name, getChallenges(authorization));
            }

            ThreadPoolExecutor challengeExecutorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(names.size());
            domainChallengeCollectionMap.forEach((name, challengeCollection) -> {
                challengeExecutorService.execute(() ->
                        challengeCollection.forEach(challenge -> doChallenge(challenge, name, hostedZoneId)));
            });

            do {
                log.info("Waiting for all challenges to complete before continuing, current active challenge threads: {}",
                        challengeExecutorService.getActiveCount());
                Thread.sleep(TimeUnit.SECONDS.toMillis(5));
            } while (challengeExecutorService.getActiveCount() > 0);
            challengeExecutorService.shutdown();

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
        String name = String.format(CHALLENGE_ENTRY_TEMPLATE, domainName);
        try {
            executeRecordSetChanges(name, digest, hostedZoneId, ChangeAction.UPSERT);
            Optional<String> recordValue;
            do {
                log.info("Waiting for name: '{}' to have txt record digest value: '{}' before triggering challenge", name, digest);
                Thread.sleep(TimeUnit.SECONDS.toMillis(60));
                recordValue = getTxtRecordValue(name);
            } while (!recordValue.isPresent() || !recordValue.get().equals(String.format("\"%s\"", digest)));
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to complete DNS 01 challenge", e);
        }

        triggerChallenge(challenge, 0);

        log.info("Deleting record: {}", name);
        executeRecordSetChanges(name, digest, hostedZoneId, ChangeAction.DELETE);
    }

    /**
     * Triggers the ACME challenge polling for its status to be complete
     *
     * @param challenge The challenge that is ready to be triggered
     */
    protected void triggerChallenge(Challenge challenge, int retryCount) {
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
            log.error("failed to trigger and wait for challenge to be accepted msg: {}", e.getMessage());
            // parallelizing challenges causes a race condition, retrying works past it an drastically improves overall time
            if (e.getMessage().startsWith("JWS has invalid anti-replay nonce")) {
                final int maxRetries = 10;
                if (retryCount < maxRetries) {
                    log.info("Retrying {} out of {}", retryCount  + 1, maxRetries);
                    triggerChallenge(challenge, retryCount + 1);
                }
            }
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

    /**
     * Uploads the required files to enable tls to identity management and s3
     *
     * @param certDir The directory containing the required files
     */
    public void uploadCertFiles(File certDir) {

        checkForRequiredFiles(certDir);

        final String caContents = getFileContents(certDir, DOMAIN_CERT_CHAIN_FILE);
        final String certContents = getFileContents(certDir, DOMAIN_CERT_FILE);
        final String keyContents = getFileContents(certDir, DOMAIN_PKCS1_KEY_FILE);
        final String pkcs8KeyContents = getFileContents(certDir, DOMAIN_PKCS8_KEY_FILE);
        final String pubKeyContents = getFileContents(certDir, DOMAIN_PUBLIC_KEY_FILE);
        final String certificateName = String.format("cerberus_%s_%s",  environmentMetadata.getName(), uuidSupplier.get());

        log.info("Uploading certificate files to IAM with name of {}.", certificateName);
        String identityManagementCertificateName = identityManagementService.uploadServerCertificate(certificateName, getPath(),
                certContents, caContents, keyContents);
        log.info("Identity Management Cert Name: {}", identityManagementCertificateName);

        log.info("Uploading certificate parts to the configuration bucket.");
        X509Certificate certificate;
        try {
            certificate = CertificateUtils.readX509Certificate(new StringInputStream(certContents));
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse x509 cert", e);
        }

        List<String> sans = new LinkedList<>();
        try {
            certificate.getSubjectAlternativeNames().forEach(o -> sans.add(o.get(1).toString()));
        } catch (Exception e) {
            log.error("Failed to parse subject alternative names from x509 cert");
        }

        CertificateInformation certificateInformation = CertificateInformation.Builder.create()
                .withIdentityManagementCertificateName(identityManagementCertificateName)
                .withCommonName(StringUtils.removeStart(certificate.getSubjectX500Principal().getName(), "CN="))
                .withSubjectAlternateNames(sans)
                .withNotBefore(new DateTime(certificate.getNotBefore(), DateTimeZone.UTC))
                .withNotAfter(new DateTime(certificate.getNotAfter(), DateTimeZone.UTC))
                .withUploaded(DateTime.now(DateTimeZone.UTC))
                .build();

        configStore.storeCert(certificateInformation, caContents, certContents, keyContents, pkcs8KeyContents, pubKeyContents);
        log.info("Successfully uploaded Certificate: {}", certificateInformation);
    }

    /**
     * Validates that the requires files to enable https for Cerberus are present see the constants at the top of this file.
     *
     * @param certDir The directory that contains the required files
     */
    private void checkForRequiredFiles(File certDir) {

        final Set<String> filenames = Sets.newHashSet();

        final FilenameFilter filter = new RegexFileFilter("^.*\\.(pem|crt)$");
        final File[] files = certDir.listFiles(filter);
        Arrays.stream(files).forEach(file -> filenames.add(file.getName()));

        if (!filenames.containsAll(EXPECTED_FILE_NAMES)) {
            final StringJoiner sj = new StringJoiner(", ", "[", "]");
            EXPECTED_FILE_NAMES.forEach(sj::add);
            throw new RuntimeException("Not all expected files are present! Expected: " + sj.toString());
        }
    }

    /**
     * Reads the data from the file for uploading
     */
    private String getFileContents(final File certDir, final String filename) {
        Preconditions.checkNotNull(certDir);
        Preconditions.checkNotNull(filename);

        File file = new File(certDir.getAbsolutePath() + File.separator + filename);
        if (file.exists() && file.canRead()) {
            try {
                return new String(Files.readAllBytes(file.toPath()), Charset.forName("UTF-8"));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read the following file: " + file.getAbsolutePath());
            }
        } else {
            throw new IllegalArgumentException("The file is not readable: " + file.getAbsolutePath());
        }
    }

    private String getPath() {
        return "/cerberus/" + environmentMetadata.getName() + "/";
    }
}
