/*
 * Copyright (c) 2016 Nike, Inc.
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

package com.nike.cerberus.domain.environment;

/**
 * Container for sensitive data needed by Cerberus.
 */
public class Secrets {

    private CmsSecrets cms = new CmsSecrets();

    private VaultSecrets vault = new VaultSecrets();

    public CmsSecrets getCms() {
        return cms;
    }

    public Secrets setCms(CmsSecrets cms) {
        this.cms = cms;
        return this;
    }

    public VaultSecrets getVault() {
        return vault;
    }

    public Secrets setVault(VaultSecrets vault) {
        this.vault = vault;
        return this;
    }
}
