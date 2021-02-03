/*
 * Copyright (c) 2019 Nike, Inc.
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

import com.google.common.collect.ImmutableList;
import com.nike.cerberus.ConfigConstants;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

/**
 * Describes the stacks that make up Cerberus.
 */
public class Stack implements Comparable<Stack> {

    public static final Stack IAM_ROLES = new Stack("iam-roles", "iam-roles.yaml", false);
    public static final Stack CONFIG = new Stack("config", "config.yaml", false);
    public static final Stack VPC = new Stack("vpc", "vpc.yaml", false);
    public static final Stack SECURITY_GROUPS = new Stack("security-groups", "security-groups.yaml", false);
    public static final Stack DATABASE = new Stack("database", "database.yaml", false);
    public static final Stack LOAD_BALANCER = new Stack("load-balancer", "load-balancer.yaml", false);
    public static final Stack INSTANCE_PROFILE = new Stack("instance-profile", "instance-profile.yaml", false);
    public static final Stack CMS = new Stack("cms", "cms-cluster.yaml", true);
    public static final Stack WAF = new Stack("web-app-firewall", "web-app-firewall.yaml", false);
    public static final Stack ROUTE53 = new Stack("route53", "route53.yaml", false);
    public static final Stack AUDIT = new Stack("audit", "audit.yaml", false);
    public static final Stack WAF_LOGGING = new Stack("waf-logging", "waf-logging.yaml", false);

    public static final ImmutableList<Stack> ALL_STACKS = ImmutableList.of(
            IAM_ROLES,
            CONFIG,
            VPC,
            SECURITY_GROUPS,
            DATABASE,
            LOAD_BALANCER,
            INSTANCE_PROFILE,
            CMS,
            WAF,
            ROUTE53,
            AUDIT,
            WAF_LOGGING
    );

    public static final ImmutableList<String> ALL_STACK_NAMES = ImmutableList.copyOf(ALL_STACKS.stream().map(Stack::getName).collect(Collectors.toList()));

    private static final String TEMPLATE_PATH_ROOT = "/cloudformation/";

    private final Logger logger = LoggerFactory.getLogger(Stack.class);


    private final String name;
    private final String templatePath;
    private final boolean needsUserData;


    private Stack(final String name,
                  final String cloudFormationFileName,
                  final boolean needsUserData) {
        this.name = name;
        this.templatePath = TEMPLATE_PATH_ROOT + cloudFormationFileName;
        this.needsUserData = needsUserData;
    }

    public String getName() {
        return name;
    }


    public String getTemplatePath() {
        return templatePath;
    }

    public boolean isNeedsUserData() {
        return needsUserData;
    }

    /**
     * Gets the template contents from the file on the classpath.
     *
     * @return Template contents
     */
    public String getTemplateText() {
        final InputStream templateStream = getClass().getResourceAsStream(templatePath);

        if (templateStream == null) {
            throw new IllegalStateException(
                    String.format("The CloudFormation JSON template doesn't exist on the classpath for stack %s, path: %s", name, templatePath));
        }

        try {
            return IOUtils.toString(templateStream, ConfigConstants.DEFAULT_ENCODING);
        } catch (final IOException e) {
            final String errorMessage = String.format("Unable to read input stream from %s", templatePath);
            logger.error(errorMessage);
            throw new RuntimeException(errorMessage, e);
        }
    }

    /**
     * Generate the CloudFormation stack name for each Cerberus component
     *
     * @param environmentName The name of the environment in which the component lives (e.g. demo, preprod, devel, etc.)
     * @return The generated CloudFormation stack name
     */
    public String getFullName(String environmentName) {
        String tokenizedEnvName = StringUtils.replaceAll(environmentName, "_", "-");
        return String.format("%s-cerberus-%s", tokenizedEnvName, name);
    }

    public static Stack fromName(final String name) {
        for (Stack stack : ALL_STACKS) {
            // ignore case and all enum style stack names
            if (stack.getName().equalsIgnoreCase(StringUtils.replaceAll(name, "_", "-"))) {
                return stack;
            }
        }
        throw new IllegalArgumentException("Unknown stack name '" + name + "', please choose from " + ALL_STACK_NAMES);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Stack stack = (Stack) o;

        if (!name.equals(stack.name)) return false;
        return templatePath.equals(stack.templatePath);

    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + templatePath.hashCode();
        return result;
    }

    @Override
    public int compareTo(Stack o) {
        return this.name.compareTo(o.name);
    }
}
