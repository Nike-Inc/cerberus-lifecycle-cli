/*
 * Copyright (c) 2020 Nike, Inc.
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

package com.nike.cerberus.domain.cloudformation;

/**
 * Represents the waf stack outputs.
 */
public class WafOutputs {

    private String autoBlockIPSetID;

    private String manualBlockIPSetID;

    private String whitelistIPSetID;

    private String webAclID;

    public String getWebAclID() {
        return webAclID;
    }

    public void setWebAclID(String webAclID) {
        this.webAclID = webAclID;
    }

    public String getAutoBlockIPSetID() {
        return autoBlockIPSetID;
    }

    public WafOutputs setAutoBlockIPSetID(String autoBlockIPSetID) {
        this.autoBlockIPSetID = autoBlockIPSetID;
        return this;
    }

    public String getManualBlockIPSetID() {
        return manualBlockIPSetID;
    }

    public WafOutputs setManualBlockIPSetID(String manualBlockIPSetID) {
        this.manualBlockIPSetID = manualBlockIPSetID;
        return this;
    }

    public String getWhitelistIPSetID() {
        return whitelistIPSetID;
    }

    public WafOutputs setWhitelistIPSetID(String whitelistIPSetID) {
        this.whitelistIPSetID = whitelistIPSetID;
        return this;
    }
}
