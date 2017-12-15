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

package com.nike.cerberus.operation.certificates;

import com.nike.cerberus.command.certificates.RotateAcmeAccountPrivateKeyCommand;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CertificateService;
import com.nike.cerberus.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class RotateAcmeAccountPrivateKeyOperation implements Operation<RotateAcmeAccountPrivateKeyCommand> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ConfigStore configStore;
    private final CertificateService certificateService;

    @Inject
    public RotateAcmeAccountPrivateKeyOperation(ConfigStore configStore,
                                                CertificateService certificateService) {
        this.configStore = configStore;
        this.certificateService = certificateService;
    }

    @Override
    public void run(RotateAcmeAccountPrivateKeyCommand command) {
        log.info("Preparing to rotate ACME account private key.");
        certificateService.rotateAcmeAccountKeyPair(command.getAcmeUrl());
        log.info("ACME account private key rotated.");
    }

    @Override
    public boolean isRunnable(RotateAcmeAccountPrivateKeyCommand command) {
        boolean isRunnable = true;

        if (!configStore.getAcmeAccountKeyPair().isPresent()) {
            log.error("There is no saved private key to rotate");
            isRunnable = false;
        }

        return isRunnable;
    }
}
