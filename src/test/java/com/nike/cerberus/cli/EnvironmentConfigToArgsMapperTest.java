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

package com.nike.cerberus.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.nike.cerberus.command.StackDelegate;
import com.nike.cerberus.command.cms.CreateCmsClusterCommand;
import com.nike.cerberus.command.cms.CreateCmsConfigCommand;
import com.nike.cerberus.command.consul.CreateConsulClusterCommand;
import com.nike.cerberus.command.core.CreateBaseCommand;
import com.nike.cerberus.command.core.UpdateStackCommand;
import com.nike.cerberus.command.core.UploadCertFilesCommand;
import com.nike.cerberus.command.core.WhitelistCidrForVpcAccessCommand;
import com.nike.cerberus.command.dashboard.PublishDashboardCommand;
import com.nike.cerberus.command.gateway.CreateCloudFrontLogProcessingLambdaConfigCommand;
import com.nike.cerberus.command.gateway.CreateGatewayClusterCommand;
import com.nike.cerberus.command.gateway.PublishLambdaCommand;
import com.nike.cerberus.command.vault.CreateVaultClusterCommand;
import com.nike.cerberus.domain.input.EnvironmentConfig;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class EnvironmentConfigToArgsMapperTest {

    private EnvironmentConfig environmentConfig;

    @Before
    public void before() throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
        InputStream yamlStream = getClass().getClassLoader().getResourceAsStream("environment.yaml");
        environmentConfig = mapper.readValue(yamlStream, EnvironmentConfig.class);
    }

    @Test
    public void test_that_mapper_copies_pre_command_flags_with_flag() {
        String commandName = "I-don't-exist";

        String[] userInput = {"--debug", "-f", "/path/to/environment.yaml", commandName, "--some-opt", "some-value"};

        String[] expected = {
                "--debug",
                "-f", "/path/to/environment.yaml",
                commandName,
        };

        String[] actual = EnvironmentConfigToArgsMapper.getArgs(environmentConfig, userInput);

        assertArgsAreEqual(expected, actual,commandName);
    }

    @Test
    public void test_that_mapper_copies_pre_command_flags_without_flag() {
        String commandName = "I-don't-exist";

        String[] userInput = {"-f", "/path/to/environment.yaml", commandName, "--some-opt", "some-value"};

        String[] expected = {
                "-f", "/path/to/environment.yaml",
                commandName,
        };

        String[] actual = EnvironmentConfigToArgsMapper.getArgs(environmentConfig, userInput);

        assertArgsAreEqual(expected, actual,commandName);
    }


    @Test
    public void test_create_base() {
        String commandName = CreateBaseCommand.COMMAND_NAME;

        String[] userInput = {"-f", "/path/to/environment.yaml", commandName};

        String[] expected = {
                "-f", "/path/to/environment.yaml",
                commandName,
                "--admin-role-arn", "arn:aws:iam::111111111:role/onelogin-roles-OneLoginAdminRole-2222222222",
                "--vpc-hosted-zone-name", "demo.internal.cerberus-oss.io",
                "--owner-email", "obvisouly.fake@nike.com",
                "--costcenter", "11111"
        };

        String[] actual = EnvironmentConfigToArgsMapper.getArgs(environmentConfig, userInput);

        assertArgsAreEqual(expected, actual,commandName);
    }

    @Test
    public void test_upload_cert_without_overwrite() {
        String commandName = UploadCertFilesCommand.COMMAND_NAME;

        String[] userInput = {"-f", "/path/to/environment.yaml", commandName, "--stack-name", "cms"};

        String[] expected = {
                "-f", "/path/to/environment.yaml",
                commandName,
                "--stack-name", "cms",
                "--cert-path", "/home/fieldju/development/cerberus_environments/demo/certs/"
        };

        String[] actual = EnvironmentConfigToArgsMapper.getArgs(environmentConfig, userInput);

        assertArgsAreEqual(expected, actual, commandName);
    }

    @Test
    public void test_upload_cert_with_overwrite() {
        String commandName = UploadCertFilesCommand.COMMAND_NAME;

        String[] userInput = {"-f", "/path/to/environment.yaml", commandName, "--stack-name", "consul", "--overwrite"};

        String[] expected = {
                "-f", "/path/to/environment.yaml",
                commandName,
                "--stack-name", "consul",
                "--cert-path", "/home/fieldju/development/cerberus_environments/demo/certs/",
                "--overwrite"
        };

        String[] actual = EnvironmentConfigToArgsMapper.getArgs(environmentConfig, userInput);

        assertArgsAreEqual(expected, actual, commandName);
    }

    @Test
    public void test_create_consul_cluster() {
        String commandName = CreateConsulClusterCommand.COMMAND_NAME;

        String[] userInput = {"-f", "/path/to/environment.yaml", commandName};

        String[] expected = {
                "-f", "/path/to/environment.yaml",
                commandName,
                StackDelegate.AMI_ID_LONG_ARG, "ami-1111",
                StackDelegate.INSTANCE_SIZE_LONG_ARG, "m3.medium",
                StackDelegate.KEY_PAIR_NAME_LONG_ARG, "cerberus-test",
                StackDelegate.COST_CENTER_LONG_ARG, "11111",
                StackDelegate.OWNER_EMAIL_LONG_ARG, "obvisouly.fake@nike.com",
                StackDelegate.OWNER_GROUP_LONG_ARG, "cloud platform engineering",
                StackDelegate.DESIRED_INSTANCES_LONG_ARG, "2",
                StackDelegate.MAX_INSTANCES_LONG_ARG, "4",
                StackDelegate.MIN_INSTANCES_LONG_ARG, "2"
        };

        String[] actual = EnvironmentConfigToArgsMapper.getArgs(environmentConfig, userInput);

        assertArgsAreEqual(expected, actual, commandName);
    }

    @Test
    public void test_create_vault_cluster() {
        String commandName = CreateVaultClusterCommand.COMMAND_NAME;

        String[] userInput = {"-f", "/path/to/environment.yaml", commandName};

        String[] expected = {
                "-f", "/path/to/environment.yaml",
                commandName,
                StackDelegate.AMI_ID_LONG_ARG, "ami-2222",
                StackDelegate.INSTANCE_SIZE_LONG_ARG, "m3.medium",
                StackDelegate.KEY_PAIR_NAME_LONG_ARG, "cerberus-test",
                StackDelegate.COST_CENTER_LONG_ARG, "11111",
                StackDelegate.OWNER_EMAIL_LONG_ARG, "obvisouly.fake@nike.com",
                StackDelegate.OWNER_GROUP_LONG_ARG, "cloud platform engineering",
                StackDelegate.DESIRED_INSTANCES_LONG_ARG, "2",
                StackDelegate.MAX_INSTANCES_LONG_ARG, "4",
                StackDelegate.MIN_INSTANCES_LONG_ARG, "2"
        };

        String[] actual = EnvironmentConfigToArgsMapper.getArgs(environmentConfig, userInput);

        assertArgsAreEqual(expected, actual, commandName);
    }

    @Test
    public void test_create_management_service_cluster() {
        String commandName = CreateCmsClusterCommand.COMMAND_NAME;

        String[] userInput = {"-f", "/path/to/environment.yaml", commandName};

        String[] expected = {
                "-f", "/path/to/environment.yaml",
                commandName,
                StackDelegate.AMI_ID_LONG_ARG, "ami-3333",
                StackDelegate.INSTANCE_SIZE_LONG_ARG, "m3.medium",
                StackDelegate.KEY_PAIR_NAME_LONG_ARG, "cerberus-test",
                StackDelegate.COST_CENTER_LONG_ARG, "11111",
                StackDelegate.OWNER_EMAIL_LONG_ARG, "obvisouly.fake@nike.com",
                StackDelegate.OWNER_GROUP_LONG_ARG, "cloud platform engineering"
        };

        String[] actual = EnvironmentConfigToArgsMapper.getArgs(environmentConfig, userInput);

        assertArgsAreEqual(expected, actual, commandName);
    }

    @Test
    public void test_create_gateway_cluster() {
        String commandName = CreateGatewayClusterCommand.COMMAND_NAME;

        String[] userInput = {"-f", "/path/to/environment.yaml", commandName};

        String[] expected = {
                "-f", "/path/to/environment.yaml",
                commandName,
                StackDelegate.AMI_ID_LONG_ARG, "ami-4444",
                StackDelegate.INSTANCE_SIZE_LONG_ARG, "m3.medium",
                StackDelegate.KEY_PAIR_NAME_LONG_ARG, "cerberus-test",
                StackDelegate.COST_CENTER_LONG_ARG, "11111",
                StackDelegate.OWNER_EMAIL_LONG_ARG, "obvisouly.fake@nike.com",
                StackDelegate.OWNER_GROUP_LONG_ARG, "cloud platform engineering",
                CreateGatewayClusterCommand.HOSTNAME_LONG_ARG, "demo.cerberis-oss.io",
                CreateGatewayClusterCommand.HOSTED_ZONE_ID_LONG_ARG, "X5CT6JROG9F2DR"
        };

        String[] actual = EnvironmentConfigToArgsMapper.getArgs(environmentConfig, userInput);

        assertArgsAreEqual(expected, actual, commandName);
    }

    @Test
    public void test_publish_dashboard() {
        String commandName = PublishDashboardCommand.COMMAND_NAME;

        String[] userInput = {"-f", "/path/to/environment.yaml", commandName};

        String[] expected = {
                "-f", "/path/to/environment.yaml",
                commandName,
                PublishDashboardCommand.ARTIFACT_URL_LONG_ARG,
                "https://github.com/Nike-Inc/cerberus-management-dashboard/releases/download/v0.8.0/cerberus-dashboard.tar.gz",
                PublishDashboardCommand.OVERRIDE_ARTIFACT_URL_LONG_ARG,
                "https://someplace.com/where/you/want/to/store/this.tar.gz"
        };

        String[] actual = EnvironmentConfigToArgsMapper.getArgs(environmentConfig, userInput);

        assertArgsAreEqual(expected, actual, commandName);
    }

    @Test
    public void test_vpc_access_whitelist() {
        String commandName = WhitelistCidrForVpcAccessCommand.COMMAND_NAME;

        String[] userInput = {"-f", "/path/to/environment.yaml", commandName};

        String[] expected = {
                "-f", "/path/to/environment.yaml",
                commandName,
                WhitelistCidrForVpcAccessCommand.CIDR_LONG_ARG, "50.39.106.150/32",
                WhitelistCidrForVpcAccessCommand.PORT_LONG_ARG, "443",
                WhitelistCidrForVpcAccessCommand.PORT_LONG_ARG, "8080",
                WhitelistCidrForVpcAccessCommand.PORT_LONG_ARG, "8200",
                WhitelistCidrForVpcAccessCommand.PORT_LONG_ARG, "8500",
                WhitelistCidrForVpcAccessCommand.PORT_LONG_ARG, "8400",
                WhitelistCidrForVpcAccessCommand.PORT_LONG_ARG, "22"
        };

        String[] actual = EnvironmentConfigToArgsMapper.getArgs(environmentConfig, userInput);

        assertArgsAreEqual(expected, actual, commandName);
    }

    @Test
    public void test_create_cms_config() {
        String commandName = CreateCmsConfigCommand.COMMAND_NAME;

        String[] userInput = {"-f", "/path/to/environment.yaml", commandName};

        String[] expected = {
                "-f", "/path/to/environment.yaml",
                commandName,
                CreateCmsConfigCommand.ADMIN_GROUP_LONG_ARG, "lst-cerberus-admins",
                CreateCmsConfigCommand.PROPERTY_SHORT_ARG, "cms.auth.connector=com.nike.cerberus.auth.connector.onelogin.OneLoginAuthConnector",
                CreateCmsConfigCommand.PROPERTY_SHORT_ARG, "auth.connector.onelogin.api_region=us",
                CreateCmsConfigCommand.PROPERTY_SHORT_ARG, "auth.connector.onelogin.client_id=123",
                CreateCmsConfigCommand.PROPERTY_SHORT_ARG, "auth.connector.onelogin.client_secret=312",
                CreateCmsConfigCommand.PROPERTY_SHORT_ARG, "auth.connector.onelogin.subdomain=nike"
        };

        String[] actual = EnvironmentConfigToArgsMapper.getArgs(environmentConfig, userInput);

        assertArgsAreEqual(expected, actual, commandName);
    }

    @Test
    public void test_update_stack_with_no_overwrite_flag_or_dyn_props() {
        String commandName = UpdateStackCommand.COMMAND_NAME;

        String[] userInput = {"-f", "/path/to/environment.yaml", commandName, EnvironmentConfigToArgsMapper.STACK_NAME_KEY, "gateway"};

        String[] expected = {
                "-f", "/path/to/environment.yaml",
                commandName,
                EnvironmentConfigToArgsMapper.STACK_NAME_KEY, "gateway",
                StackDelegate.AMI_ID_LONG_ARG, "ami-4444",
                StackDelegate.INSTANCE_SIZE_LONG_ARG, "m3.medium",
                StackDelegate.KEY_PAIR_NAME_LONG_ARG, "cerberus-test",
                StackDelegate.COST_CENTER_LONG_ARG, "11111",
                StackDelegate.OWNER_EMAIL_LONG_ARG, "obvisouly.fake@nike.com",
                StackDelegate.OWNER_GROUP_LONG_ARG, "cloud platform engineering",
        };

        String[] actual = EnvironmentConfigToArgsMapper.getArgs(environmentConfig, userInput);

        assertArgsAreEqual(expected, actual, commandName);
    }

    @Test
    public void test_update_stack_with_overwrite_flag_and_dyn_props() {
        String commandName = UpdateStackCommand.COMMAND_NAME;

        String[] userInput = {"-f", "/path/to/environment.yaml", commandName, EnvironmentConfigToArgsMapper.STACK_NAME_KEY, "gateway", "--overwrite-template", "-P", "k=v"};

        String[] expected = {
                "-f", "/path/to/environment.yaml",
                commandName,
                EnvironmentConfigToArgsMapper.STACK_NAME_KEY, "gateway",
                StackDelegate.AMI_ID_LONG_ARG, "ami-4444",
                StackDelegate.INSTANCE_SIZE_LONG_ARG, "m3.medium",
                StackDelegate.KEY_PAIR_NAME_LONG_ARG, "cerberus-test",
                StackDelegate.COST_CENTER_LONG_ARG, "11111",
                StackDelegate.OWNER_EMAIL_LONG_ARG, "obvisouly.fake@nike.com",
                StackDelegate.OWNER_GROUP_LONG_ARG, "cloud platform engineering",
                UpdateStackCommand.OVERWRITE_TEMPLATE_LONG_ARG,
                UpdateStackCommand.PARAMETER_SHORT_ARG, "k=v",
        };

        String[] actual = EnvironmentConfigToArgsMapper.getArgs(environmentConfig, userInput);

        assertArgsAreEqual(expected, actual, commandName);
    }

    @Test
    public void test_publish_lambda_cf_sg_ip() {
        String commandName = PublishLambdaCommand.COMMAND_NAME;

        String[] userInput = {"-f", "/path/to/environment.yaml", PublishLambdaCommand.COMMAND_NAME, PublishLambdaCommand.LAMBDA_NAME_LONG_ARG, "CLOUD_FRONT_SG_GROUP_IP_SYNC"};

        String[] expected = {
                "-f", "/path/to/environment.yaml",
                commandName,
                PublishLambdaCommand.LAMBDA_NAME_LONG_ARG, "CLOUD_FRONT_SG_GROUP_IP_SYNC",
                PublishLambdaCommand.ARTIFACT_URL_LONG_ARG, "https://github.com/Nike-Inc/cerberus-lifecycle-cli/raw/master/update_security_groups.zip"
        };

        String[] actual = EnvironmentConfigToArgsMapper.getArgs(environmentConfig, userInput);

        assertArgsAreEqual(expected, actual, commandName);
    }

    @Test
    public void test_create_cloud_front_log_processor_lambda_config() {
        String commandName = CreateCloudFrontLogProcessingLambdaConfigCommand.COMMAND_NAME;

        String[] userInput = {"-f", "/path/to/environment.yaml", commandName};

        String[] expected = {
                "-f", "/path/to/environment.yaml",
                commandName,
                CreateCloudFrontLogProcessingLambdaConfigCommand.RATE_LIMIT_PER_MINUTE_LONG_ARG, "100",
                CreateCloudFrontLogProcessingLambdaConfigCommand.RATE_LIMIT_VIOLATION_BLOCK_PERIOD_IN_MINUTES_LONG_ARG, "60",
                CreateCloudFrontLogProcessingLambdaConfigCommand.SLACK_WEB_HOOK_URL_LONG_ARG, "https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX",
                CreateCloudFrontLogProcessingLambdaConfigCommand.SLACK_ICON_LONG_ARG, "https://raw.githubusercontent.com/Nike-Inc/cerberus/master/images/cerberus-github-logo-black-filled-circle%40500px.png",
                CreateCloudFrontLogProcessingLambdaConfigCommand.GOOGLE_ANALYTICS_TRACKING_ID_LONG_ARG, "abc123"
        };

        String[] actual = EnvironmentConfigToArgsMapper.getArgs(environmentConfig, userInput);

        assertArgsAreEqual(expected, actual, commandName);
    }

    @Test
    public void test_publish_lambda_waf() {
        String commandName = PublishLambdaCommand.COMMAND_NAME;

        String[] userInput = {"-f", "/path/to/environment.yaml", PublishLambdaCommand.COMMAND_NAME, PublishLambdaCommand.LAMBDA_NAME_LONG_ARG, "WAF"};

        String[] expected = {
                "-f", "/path/to/environment.yaml",
                commandName,
                PublishLambdaCommand.LAMBDA_NAME_LONG_ARG, "WAF",
                PublishLambdaCommand.ARTIFACT_URL_LONG_ARG, "https://github.com/Nike-Inc/cerberus-cloudfront-lambda/releases/download/v1.1.0/cerberus-cloudfront-lambda.jar"
        };

        String[] actual = EnvironmentConfigToArgsMapper.getArgs(environmentConfig, userInput);

        assertArgsAreEqual(expected, actual, commandName);
    }

    private void assertArgsAreEqual(String[] expected, String[] actual, String commandName) {
        if (expected.length != actual.length) {
            fail(String.format("The args not the same length, expected length: %s, actual length: %s\nExpected Args: %s\nActual Args:   %s", expected.length, actual.length, String.join(" ", expected), String.join(" ", actual)));
        }

        // the args up to and including the command should be the same
        int commandIndex = -1;
        for (int i = 0; i < expected.length; i++) {
            String e = expected[i];
            String a = actual[i];
            assertEquals(e, a);
            if (e.equals(commandName)) {
                commandIndex = i;
                break;
            }
        }

        // everything after the command name is not guaranteed to be in the same order
        // so we need to ensure that the pairs are in expected are also in the actual, as well as the single arg switches
        // we will do an n^2 brute force check
        for (int i = commandIndex + 1; i < expected.length; i++) {
            String eKey = expected[i];

            boolean isPairE = false;
            String eVal = "";
            if (i < expected.length - 1) {
                eVal = expected[i+1];
                if (! StringUtils.startsWith(eVal, "-")) {
                    isPairE = true;
                }
            }

            if (isPairE) {
                i++;
            }

            boolean found = false;
            for (int n = commandIndex + 1; n < actual.length; n++) {
                String aKey = actual[n];

                boolean isPairA = false;
                String aVal = "";
                if (n < actual.length - 1) {
                    aVal = actual[n+1];
                    if (! StringUtils.startsWith(aVal, "-")) {
                        isPairA = true;
                    }
                }

                if (isPairA) {
                    n++;
                }

                if (isPairE && isPairA && eKey.equals(aKey) && eVal.equals(aVal)) {
                    found = true;
                    break;
                } else if (! isPairE && ! isPairA && eKey.equals(aKey)) {
                    found = true;
                    break;
                }
            }
            if (! found) {
                if (isPairE) {
                    fail(String.format("Failed to find [ %s %s ] in actual args \nactual args:   %s\nexpected args: %s\nactual size: %s\nexpected size: %s", eKey, eVal, String.join(" ", actual), String.join(" ", expected), actual.length, expected.length));
                } else {
                    fail(String.format("Failed to find [ %s ] in actual args \nactual args:   %s\nexpected args: %s\nactual size: %s\nexpected size: %s", eKey, String.join(" ", actual), String.join(" ", expected), actual.length, expected.length));
                }
            }
        }
    }
}
