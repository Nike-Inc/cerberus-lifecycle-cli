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

import com.google.common.collect.ImmutableList;
import com.nike.cerberus.ConfigConstants;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Describes the stacks that make up Cerberus.
 */
public class Stack {

    public static final Stack BASE = new Stack("base", "base.yaml");
    public static final Stack VAULT = new Stack("vault", null);
    public static final Stack CMS = new Stack("cms", "cms-cluster.yaml");
    public static final Stack GATEWAY = new Stack("gateway", null);
    public static final Stack VPC = new Stack("vpc", "vpc.yaml");
    public static final Stack DATABASE = new Stack("database", "database.yaml");
    public static final Stack SECURITY_GROUPS = new Stack("security-groups", "security-groups.yaml");
    public static final Stack LOAD_BALANCER = new Stack("load-balancer", "load-balancer.yaml");
    public static final Stack ROUTE53 = new Stack("route53", "route53.yaml");
    public static final Stack WAF = new Stack("web-app-firewall", "web-app-firewall.yaml");

    public static final ImmutableList<Stack> ALL_STACKS = ImmutableList.of(BASE, VAULT, CMS, GATEWAY, VPC, DATABASE, SECURITY_GROUPS, LOAD_BALANCER, ROUTE53, WAF);

    private static final String TEMPLATE_PATH_ROOT = "/cloudformation/";

    private final Logger logger = LoggerFactory.getLogger(Stack.class);


    private final String name;
    private final String templatePath;

    private Stack(final String name, final String cloudFormationFileName) {
        this.name = name;
        this.templatePath = TEMPLATE_PATH_ROOT + cloudFormationFileName;
    }

    public String getName() {
        return name;
    }

    public String getTemplatePath() {
        return templatePath;
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
                    String.format("The CloudFormation JSON template doesn't exist on the classpath. path: %s", templatePath));
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
        return String.format("%s-cerberus-%s", environmentName, name);
    }

    public static Stack fromName(final String name) {
        for (Stack stack : ALL_STACKS) {
            if (stack.getName().equalsIgnoreCase(StringUtils.replaceAll(name,"_", "-"))) {
                return stack;
            }
        }

        throw new IllegalArgumentException("Unknown stack name: " + name);
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
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + templatePath.hashCode();
        return result;
    }
}
