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

package com.nike.cerberus;

import com.google.common.collect.ImmutableSet;

public class ConfigConstants {

    public static final String ENV_PREFIX = "cerberus-";

    public static final String DEFAULT_ENCODING = "UTF-8";

    public static final int MINIMUM_AZS = 3;

    public static final String CONFIG_BUCKET_KEY = "cerberusconfigbucket";

    public static final String DEFAULT_CMS_DB_NAME = "cms";

    public static final String ENV_DATA_FILE = "config/environment.json";

    public static final String SECRETS_DATA_FILE = "config/secrets.json";

    public static final String CERT_PART_CA = "ca.pem";

    public static final String CERT_PART_CERT = "cert.pem";

    public static final String CERT_PART_PKCS8_KEY = "pkcs8-key.pem";

    public static final String CERT_PART_KEY = "key.pem";

    public static final String CERT_PART_PUBKEY = "pubkey.pem";

    public static final String BASE_STACK_NAME = "base";

    public static final String BASE_STACK_TEMPLATE_PATH = "/cloudformation/base.yaml";

    public static final String CMS_STACK_TEMPLATE_PATH = "/cloudformation/cms-cluster.json";

    public static final String DATABASE_STACK_TEMPLATE_PATH = "/cloudformation/database.yaml";

    public static final String LOAD_BALANCER_STACK_TEMPLATE_PATH = "/cloudformation/load-balancer.yaml";

    public static final String ROUTE53_TEMPLATE_PATH = "/cloudformation/route53.yaml";

    public static final String SECURITY_GROUPS_STACK_TEMPLATE_PATH = "/cloudformation/security-groups.yaml";

    public static final String VPC_STACK_TEMPLATE_PATH = "/cloudformation/vpc.yaml";

    public static final String WAF_STACK_TEMPLATE_PATH = "/cloudformation/web-app-firewall.yaml";

    public static final String CMS_ENV_CONFIG_PATH = "data/cms/environment.properties";

    public static final String VERSION_PROPERTY = "cli.version";

    public static final String CMS_ADMIN_GROUP_KEY = "cms.admin.group";

    public static final String ROOT_USER_ARN_KEY = "root.user.arn";

    public static final String ADMIN_ROLE_ARN_KEY = "admin.role.arn";

    public static final String CMS_ROLE_ARN_KEY = "cms.role.arn";

    public static final String JDBC_URL_KEY = "JDBC.url";

    public static final String JDBC_USERNAME_KEY = "JDBC.username";

    public static final String JDBC_PASSWORD_KEY ="JDBC.password";

    public static final String CMK_ARNS_KEY = "cms.encryption.cmk.arns";

    public static final String HASH_SALT = "cms.auth.token.hash.salt";

    public static final String CMS_ENV_NAME = "cms.env.name";

    public static final ImmutableSet<String> SYSTEM_CONFIGURED_CMS_PROPERTIES = ImmutableSet.of(
            ROOT_USER_ARN_KEY,
            ADMIN_ROLE_ARN_KEY,
            CMS_ROLE_ARN_KEY,
            JDBC_URL_KEY,
            JDBC_USERNAME_KEY,
            JDBC_PASSWORD_KEY,
            CMK_ARNS_KEY,
            HASH_SALT);

    public static final String CERBERUS_AMI_TAG_NAME = "tag:cerberus_component";

    public static final String CMS_AMI_TAG_VALUE = "cms";

    public static final String SKIP_AMI_TAG_CHECK_ARG = "--skip-ami-tag-check";

    public static final String SKIP_AMI_TAG_CHECK_DESCRIPTION = "Skip validation of 'cerberus_component' tag on AMI";

    public static final String AMI_TAG_CHECK_ERROR_MESSAGE
                        = "FAIL: AMI tag check failed!\n"
                        + "Given AMI ID either does not exist "
                        + "or does not contain cerberus tag 'cerberus_component' with stack name "
                        + "or cerberus tag does not match the stack that is being deployed.\n"
                        + "Please refer documentation on AMI Tagging or use '"+ SKIP_AMI_TAG_CHECK_ARG +"' option to skip this check.";
}