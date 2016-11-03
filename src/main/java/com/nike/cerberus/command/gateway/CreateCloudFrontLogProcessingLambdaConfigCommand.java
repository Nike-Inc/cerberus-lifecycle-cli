package com.nike.cerberus.command.gateway;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.gateway.CreateCloudFrontLogProcessingLambdaConfigOperation;

import static com.nike.cerberus.command.gateway.CreateCloudFrontLogProcessingLambdaConfigCommand.COMMAND_NAME;

/**
 * Command for creating config file for Cloud Front Log Processing Lambda
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Creates Config file needed to drive CloudFront log processing lambda.")
public class CreateCloudFrontLogProcessingLambdaConfigCommand implements Command {

    public static final String COMMAND_NAME = "create-cloud-front-log-processor-lambda-config";

    @Parameter(names = "--rate-limit-per-minute", description = "The maximum number of requests from an IP per minute that can flow through the gateway before being auto blocked.", required = true)
    private Integer requestPerMinuteLimit;

    @Parameter(names = "--rate-limit-violation-block-period-in-minutes", description = "Time in minutes to block an ip that violates the rate limit.", required = true)
    private Integer rateLimitViolationBlacklistPeriodInMinutes;

    @Parameter(names = "--slack-web-hook-url", description = "If you provide a web hook url for slack the lambda will send messages on errors and summary info.")
    private String slackWebHookUrl;

    @Parameter(names = "--slack-icon", description = "If you provide an emoji or an icon url the lambda will use it when sending messages.")
    private String slackIcon;

    public Integer getRequestPerMinuteLimit() {
        return requestPerMinuteLimit;
    }

    public Integer getRateLimitViolationBlacklistPeriodInMinutes() {
        return rateLimitViolationBlacklistPeriodInMinutes;
    }

    public String getSlackWebHookUrl() {
        return slackWebHookUrl;
    }

    public String getSlackIcon() {
        return slackIcon;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateCloudFrontLogProcessingLambdaConfigOperation.class;
    }
}
