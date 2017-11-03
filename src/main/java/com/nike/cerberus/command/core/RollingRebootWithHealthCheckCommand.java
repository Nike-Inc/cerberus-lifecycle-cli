package com.nike.cerberus.command.core;

import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.github.tomaslanger.chalk.Chalk;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.service.AutoScalingService;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.Ec2Service;
import com.nike.cerberus.store.ConfigStore;
import com.nike.vault.client.http.HttpStatus;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.nike.cerberus.command.core.RollingRebootWithHealthCheckCommand.COMMAND_NAME;
import static com.nike.cerberus.service.CloudFormationService.MIN_INSTANCES_STACK_PARAMETER_KEY;
import static com.nike.cerberus.service.Ec2Service.EC2_ASG_GROUP_NAME_TAG_KEY;
import static com.nike.cerberus.service.Ec2Service.INSTANCE_STATE_FILTER_NAME;
import static com.nike.cerberus.service.Ec2Service.INSTANCE_STATE_RUNNING_FILTER_VALUE;

/**
 * Command to reboot the CMS cluster.
 */
@Parameters(commandNames = COMMAND_NAME,
        commandDescription = "Performs a safe rolling reboot on instances in the given cluster, checking that " +
                "the previous instance is healthy before rebooting the next one.")
public class RollingRebootWithHealthCheckCommand implements Command {

    public static final String COMMAND_NAME = "rolling-reboot";

    @Parameter(names = {"--stack-name"}, required = true, description = "The stack name to reboot.")
    private StackName stackName = StackName.CMS;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final static ImmutableMap<String, String> HEALTH_CHECK_MAP = ImmutableMap.of(
            StackName.CMS.getName(), "http://%s:8080/healthcheck"
    );

    private final static int DEFAULT_HTTP_TIMEOUT = 15;

    private final static TimeUnit DEFAULT_HTTP_TIMEOUT_UNIT = TimeUnit.SECONDS;

    private final static int NUM_SECS_BETWEEN_HEALTH_CHECKS = 5;

    private final static int EXPECTED_NUM_SUCCESSES_AFTER_REBOOT = 10;

    private final static int EXPECTED_NUM_FAILURES_AFTER_REBOOT = 3;

    private final static int EXPECTED_NUM_SUCCESSES_BEFORE_REBOOT = 1;

    private final static int HEALTH_CHECK_FAILED_CODE = -1;

    private final ConfigStore configStore;

    private final CloudFormationService cloudFormationService;

    private final Ec2Service ec2Service;

    private final AutoScalingService autoScalingService;

    private final Proxy proxy;

    @Inject
    public RollingRebootWithHealthCheckCommand(final ConfigStore configStore,
                                               final CloudFormationService cloudFormationService,
                                               final Ec2Service ec2Service,
                                               final AutoScalingService autoScalingService,
                                               final Proxy proxy) {
        this.configStore = configStore;
        this.cloudFormationService = cloudFormationService;
        this.ec2Service = ec2Service;
        this.autoScalingService = autoScalingService;
        this.proxy = proxy;
    }

    @Override
    public void execute() {

        logger.warn(Chalk.on(
                "If this command fails: the minimum instance size may need to be increased and an EC2 instance" +
                        " may need to be set to 'in-service' state on the auto scaling group").yellow().toString());

        final String stackId = configStore.getStackId(stackName);
        final Map<String, String> stackOutputs = cloudFormationService.getStackOutputs(stackId);

        final Map<String, String> stackParameters = cloudFormationService.getStackParameters(stackId);
        final int minInstances = Integer.parseInt(stackParameters.get(MIN_INSTANCES_STACK_PARAMETER_KEY));

        final String autoScalingGroupId = stackOutputs.get(CloudFormationService.AUTO_SCALING_GROUP_LOGICAL_ID_OUTPUT_KEY);
        logger.debug("Found auto scaling group id for stack: {}", stackId);

        final Filter isRunningFilter = new Filter(INSTANCE_STATE_FILTER_NAME).withValues(INSTANCE_STATE_RUNNING_FILTER_VALUE);
        final List<Instance> instances = ec2Service.getInstancesByTag(EC2_ASG_GROUP_NAME_TAG_KEY, autoScalingGroupId, isRunningFilter);
        logger.debug("Found {} instances by tag: '{}:{}'", instances.size(), EC2_ASG_GROUP_NAME_TAG_KEY, autoScalingGroupId);

        logger.info("Temporarily decreasing min instances for ASG: {}", autoScalingGroupId);
        autoScalingService.updateMinInstancesForAutoScalingGroup(autoScalingGroupId, minInstances - 1);

        instances.forEach(instance -> rebootInstance(stackName, autoScalingGroupId, instance));

        logger.info("Increasing min instances for ASG: {}", autoScalingGroupId);
        autoScalingService.updateMinInstancesForAutoScalingGroup(autoScalingGroupId, minInstances);
    }

    /**
     * Reboot an instance and make sure it comes back healthy
     */
    private void rebootInstance(StackName stackName, String autoScalingGroupId, Instance instance) {

        final String healthCheckUrlTmpl = HEALTH_CHECK_MAP.get(stackName.getName());
        final String healthCheckUrl = String.format(healthCheckUrlTmpl, instance.getPublicDnsName());

        logger.info("Checking that instance health check is reachable...");
        waitForHealthCheckStatusCode(healthCheckUrl, HttpStatus.OK, EXPECTED_NUM_SUCCESSES_BEFORE_REBOOT);

        final String instanceId = instance.getInstanceId();
        logger.info("Setting instance state to standby: {}", instanceId);
        autoScalingService.setInstanceStateToStandby(autoScalingGroupId, instanceId);

        logger.info("Rebooting instance: {}", instanceId);
        ec2Service.rebootEc2Instance(instanceId);

        // wait for health check fail to confirm box reboot
        logger.info("Waiting for health check failure to confirm reboot...");
        waitForHealthCheckStatusCode(healthCheckUrl, HEALTH_CHECK_FAILED_CODE, EXPECTED_NUM_FAILURES_AFTER_REBOOT);

        // wait for health check pass to confirm instance is healthy after reboot
        logger.warn(Chalk.on(
                "If a proxy is required to talk to the EC2 instance, then make sure it is set up." +
                        " Otherwise this command will never succeed.").yellow().toString());
        logger.info("Waiting for health check to pass again to confirm instance is healthy...");
        waitForHealthCheckStatusCode(healthCheckUrl, HttpStatus.OK, EXPECTED_NUM_SUCCESSES_AFTER_REBOOT);

        logger.info("Setting instance state to in-service: {}", instanceId);
        autoScalingService.setInstanceStateToInService(autoScalingGroupId, instanceId);
    }

    /**
     * Poll the health check 'n' times, looking for the given response
     * @param healthCheckUrl - The health check URL
     * @param numConsecutiveResponsesExpected - The number of times to poll health check
     */
    private void waitForHealthCheckStatusCode(final String healthCheckUrl,
                                              final long expectedStatusCode,
                                              final int numConsecutiveResponsesExpected) {

        int responseCode;
        int consecutiveResponses = 0;
        while (consecutiveResponses < numConsecutiveResponsesExpected) {

            responseCode = executeHealthCheck(healthCheckUrl);

            if (responseCode == expectedStatusCode) {
                consecutiveResponses++;
            } else if (consecutiveResponses > 0) {
                final String message = Chalk.on("Instance health check did not repeat response code ({}), {} times").red().bold().toString();
                logger.debug(message, expectedStatusCode, numConsecutiveResponsesExpected);
                consecutiveResponses = 0;
            }

            try {
                TimeUnit.SECONDS.sleep(NUM_SECS_BETWEEN_HEALTH_CHECKS);
            } catch (InterruptedException ie) {
                logger.error(Chalk.on("Timeout between health checks has been interrupted").red().bold().toString());
                return;
            }
        }
    }

    /**
     * Execute the given health check
     * @param healthCheckUrl - Name of that EC2 instance belongs to
     * @return - Response code of the health check
     */
    private int executeHealthCheck(final String healthCheckUrl) {

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .hostnameVerifier(new NoopHostnameVerifier())
                .proxy(proxy)
                .connectTimeout(DEFAULT_HTTP_TIMEOUT, DEFAULT_HTTP_TIMEOUT_UNIT)
                .writeTimeout(DEFAULT_HTTP_TIMEOUT, DEFAULT_HTTP_TIMEOUT_UNIT)
                .readTimeout(DEFAULT_HTTP_TIMEOUT, DEFAULT_HTTP_TIMEOUT_UNIT)
                .build();

        final Request requestBuilder = new Request.Builder()
                .url(healthCheckUrl)
                .get()
                .build();

        final Call healthCheckCall = okHttpClient.newCall(requestBuilder);

        try(final Response response = healthCheckCall.execute()) {
            logger.debug("Health check returned status: {}, URL: {}", response.code(), healthCheckUrl);
            return response.code();
        } catch (IOException ioe) {
            final String message = Chalk.on("Health check failed, Cause: \"{}\", URL: {}").red().toString();
            logger.debug(message, ioe.getMessage(), healthCheckUrl);
        }

        return HEALTH_CHECK_FAILED_CODE;
    }

    @Override
    public boolean isRunnable() {
        final String stackNameStr = stackName.getName();
        final String stackId = configStore.getStackId(stackName);
        final Map<String, String> stackParameters = cloudFormationService.getStackParameters(stackId);

        if (! HEALTH_CHECK_MAP.containsKey(stackNameStr)) {
            logger.error("Cannot reboot cluster: {}. Allowed stacks: {}", stackName, HEALTH_CHECK_MAP.keySet());
            return false;
        } else if (! stackParameters.containsKey(MIN_INSTANCES_STACK_PARAMETER_KEY)) {
            logger.error("Could not find parameter 'minInstances' on stack: {}", stackId);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }
}
