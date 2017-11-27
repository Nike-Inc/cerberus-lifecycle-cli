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

import com.google.inject.Inject;
import com.nike.cerberus.command.core.DeleteOldestCertificatesCommand;
import com.nike.cerberus.domain.environment.CertificateInformation;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CertificateService;
import com.nike.cerberus.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DeleteOldestCertificatesOperation implements Operation<DeleteOldestCertificatesCommand> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final ConfigStore configStore;
    private final CertificateService certificateService;

    @Inject
    public DeleteOldestCertificatesOperation(ConfigStore configStore,
                                             CertificateService certificateService) {

        this.configStore = configStore;
        this.certificateService = certificateService;
    }

    @Override
    public void run(DeleteOldestCertificatesCommand command) {
        List<CertificateInformation> certificateInformationList = configStore.getCertificationInformationList();
        int indexBeforeLast = certificateInformationList.size() - 1;
        certificateInformationList.subList(0, indexBeforeLast).forEach(certificateInformation -> {
            certificateService.deleteCertificate(certificateInformation.getCertificateName());
        });
    }

    @Override
    public boolean isRunnable(DeleteOldestCertificatesCommand command) {
        List<CertificateInformation> certificateInformationList = configStore.getCertificationInformationList();

        boolean isRunnable = true;

        if (certificateInformationList.size() < 2) {
            log.error("The certificate list did not have at least 2 certs, " +
                    "cannot delete oldest certs, aborting...");
            isRunnable = false;
         }

        return isRunnable;
    }
}
