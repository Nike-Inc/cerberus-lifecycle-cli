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
 * Describes the stacks that make up Cerberus.
 */
public enum StackName {
    BASE("base"),
    VAULT("vault"),
    CMS("cms"),
    GATEWAY("gateway"),
    VPC("vpc"),
    DATABASE("database"),
    SECURITY_GROUPS("security-groups"),
    LOAD_BALANCER("load-balancer"),
    ROUTE53("route53"),
    WAF("web-app-firewall"),
    @Deprecated
    RDSBACKUP("rdsbackup"), // TODO: need to remove but casually deleting will cause JSON parse error
    @Deprecated
    DEPRECATED_BASE("base");  // TODO: remove this class when old backup and restore is removed

    private final String name;

    StackName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Generate the CloudFormation stack name for each Cerberus component
     * @param environmentName  The name of the environment in which the component lives (e.g. demo, preprod, devel, etc.)
     * @return  The generated CloudFormation stack name
     */
    public String getFullName(String environmentName) {
        return String.format("%s-cerberus-%s", environmentName, name);
    }

    public static StackName fromName(final String name) {
        for (StackName stackName : StackName.values()) {
            if (stackName.getName().equals(name)) {
                return stackName;
            }
        }

        throw new IllegalArgumentException("Unknown stack name: " + name);
    }
}
