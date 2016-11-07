/*
 * Copyright (c) 2016 Nike Inc.
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

package com.nike.cerberus.domain.template;

/**
 * Consul configuration input.
 */
public class ConsulConfigurationInput {

    private String aclMasterToken;

    private String gossipEncryptionToken;

    private String datacenter;

    public String getAclMasterToken() {
        return aclMasterToken;
    }

    public ConsulConfigurationInput setAclMasterToken(String aclMasterToken) {
        this.aclMasterToken = aclMasterToken;
        return this;
    }

    public String getGossipEncryptionToken() {
        return gossipEncryptionToken;
    }

    public ConsulConfigurationInput setGossipEncryptionToken(String gossipEncryptionToken) {
        this.gossipEncryptionToken = gossipEncryptionToken;
        return this;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public ConsulConfigurationInput setDatacenter(String datacenter) {
        this.datacenter = datacenter;
        return this;
    }
}
