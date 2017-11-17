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

import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.model.Certificate;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.RemoveListenerCertificatesRequest;
import com.google.inject.Inject;
import com.nike.cerberus.command.core.AddCertificateToAlbCommand;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.IdentityManagementService;
import com.nike.cerberus.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddCertificateToAlbOperation implements Operation<AddCertificateToAlbCommand> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AmazonElasticLoadBalancing amazonElasticLoadBalancing;
    private final IdentityManagementService identityManagementService;
    private final ConfigStore configStore;

    @Inject
    public AddCertificateToAlbOperation(AmazonElasticLoadBalancing amazonElasticLoadBalancing,
                                        IdentityManagementService identityManagementService,
                                        ConfigStore configStore) {

        this.amazonElasticLoadBalancing = amazonElasticLoadBalancing;
        this.identityManagementService = identityManagementService;
        this.configStore = configStore;
    }

    @Override
    public void run(AddCertificateToAlbCommand command) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public boolean isRunnable(AddCertificateToAlbCommand command) {
        return true;
    }
}
