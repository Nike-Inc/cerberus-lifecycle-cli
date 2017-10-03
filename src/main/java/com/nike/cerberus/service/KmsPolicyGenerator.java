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

import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.actions.KMSActions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generate a Key Policy for Customer Master Key (CMK) in KMS.
 */
public class KmsPolicyGenerator {

    private static final String AWS_PROVIDER = "AWS";
    private static final String KEY_ADMINS = "Key admins can manage this CMK";
    private static final String CMS_USER = "CMS can use this CMK";

    private static final Principal[] NONE = new Principal[0];

    private String description;
    private List<String> adminArns;
    private String cmsArn;

    /**
     * Sid description
     */
    public KmsPolicyGenerator withDescription(String description) {
        this.description = description;
        return this;
    }

    public KmsPolicyGenerator withAdminArns(List<String> adminArns) {
        this.adminArns = adminArns;
        return this;
    }

    public KmsPolicyGenerator withCmsArn(String cmsArn) {
        this.cmsArn = cmsArn;
        return this;
    }

    /**
     * Generate policy as a JSON String
     */
    public String generatePolicyJson() {
        Policy policy = generatePolicy();

        // Let AWS libraries generate the JSON
        String awsJson = policy.toJson();

        // Pretty print the AWS JSON for our users
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(gson.fromJson(awsJson, JsonObject.class));
    }

    /**
     * Generate a Key Policy
     */
    protected Policy generatePolicy() {
        Policy policy = new Policy();
        policy.withId(description);

        Statement keyAdminsStatement = new Statement(Statement.Effect.Allow);
        keyAdminsStatement.withId(KEY_ADMINS);
        keyAdminsStatement.withPrincipals(arnsToPrincipals(adminArns));
        keyAdminsStatement.withActions(KMSActions.AllKMSActions);
        keyAdminsStatement.withResources(new Resource("*"));

        Statement cmsStatement = new Statement(Statement.Effect.Allow);
        cmsStatement.withId(CMS_USER);
        cmsStatement.withPrincipals(arnToPrincipal(cmsArn));
        cmsStatement.withActions(
                // To Use the Key
                KMSActions.Encrypt,
                KMSActions.Decrypt,
                KMSActions.ReEncryptTo,
                KMSActions.ReEncryptFrom,
                KMSActions.GenerateDataKey,
                KMSActions.GenerateDataKeyWithoutPlaintext
        );
        cmsStatement.withResources(new Resource("*"));

        policy.withStatements(
                keyAdminsStatement,
                cmsStatement
        );

        return policy;
    }

    /**
     * Convert ARNs to principal objects
     */
    protected Principal[] arnsToPrincipals(List<String> arns) {
        if (arns.isEmpty()) {
            return NONE;
        } else {
            return arns.stream()
                    .map(this::arnToPrincipal)
                    .collect(Collectors.toList())
                    .toArray(NONE);
        }
    }

    protected Principal arnToPrincipal(String arn) {
        return new Principal(AWS_PROVIDER, arn, false);
    }
}
