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
 * Represents sensitive information used by Consul.
 */
public class ConsulSecrets {

    private String gossipEncryptionToken;

    private String aclMasterToken;

    private String vaultAclToken;

    public String getGossipEncryptionToken() {
        return gossipEncryptionToken;
    }

    public ConsulSecrets setGossipEncryptionToken(String gossipEncryptionToken) {
        this.gossipEncryptionToken = gossipEncryptionToken;
        return this;
    }

    public String getAclMasterToken() {
        return aclMasterToken;
    }

    public ConsulSecrets setAclMasterToken(String aclMasterToken) {
        this.aclMasterToken = aclMasterToken;
        return this;
    }

    public String getVaultAclToken() {
        return vaultAclToken;
    }

    public ConsulSecrets setVaultAclToken(String vaultAclToken) {
        this.vaultAclToken = vaultAclToken;
        return this;
    }
}
