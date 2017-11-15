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

package com.nike.cerberus.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.ListStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackResourceSummary;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.amazonaws.services.cloudformation.waiters.AmazonCloudFormationWaiters;
import com.amazonaws.waiters.Waiter;
import com.amazonaws.waiters.WaiterHandler;
import com.amazonaws.waiters.WaiterParameters;
import com.beust.jcommander.internal.Maps;
import com.github.tomaslanger.chalk.Chalk;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.UnexpectedCloudFormationStatusException;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service wrapper for AWS CloudFormation.
 */
public class CloudFormationService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public final static String AUTO_SCALING_GROUP_LOGICAL_ID_OUTPUT_KEY = "autoscalingGroupLogicalId";

    public final static String MIN_INSTANCES_STACK_PARAMETER_KEY = "minimumInstances";

    private final AmazonCloudFormation cloudFormationClient;

    private final EnvironmentMetadata environmentMetadata;

    private final AmazonCloudFormationWaiters waiters;

    @Inject
    public CloudFormationService(final AmazonCloudFormation cloudFormationClient,
                                 final EnvironmentMetadata environmentMetadata) {
        this.cloudFormationClient = cloudFormationClient;
        this.environmentMetadata = environmentMetadata;

        waiters = new AmazonCloudFormationWaiters(cloudFormationClient);
    }

    /**
     * Creates a new stack.
     *
     * @param parameters Input parameters.
     * @return Stack ID
     */
    public String createStackAndWait(final Stack stack,
                                     final Map<String, String> parameters,
                                     final boolean iamCapabilities,
                                     final Map<String, String> globalTags) {

        String stackName = stack.getFullName(environmentMetadata.getName());

        logger.info(String.format("Executing the Cloud Formation: %s, Stack Name: %s", stack.getTemplatePath(), stackName));

        final CreateStackRequest request = new CreateStackRequest()
                .withStackName(stack.getFullName(environmentMetadata.getName()))
                .withParameters(convertParameters(parameters))
                .withTemplateBody(stack.getTemplateText())
                .withTags(getTags(globalTags));

        if (iamCapabilities) {
            request.getCapabilities().add("CAPABILITY_IAM");
        }

        final CreateStackResult result = cloudFormationClient.createStack(request);

        waitAndPrintCFEvents(stackName, waiters.stackCreateComplete());

        return result.getStackId();
    }

    /**
     * Uses AWS CF Aync Waiters to wait for Cloud Formation actions to complete, while logging events and verifying success
     * @param stackName The stack that is having an action performed
     * @param waiter The Amazon waiter
     */
    private void waitAndPrintCFEvents(String stackName, Waiter waiter) {
        SuccessTrackingWaiterHandler handler = new SuccessTrackingWaiterHandler();

        @SuppressWarnings("unchecked")
        Future future = waiter
                .runAsync(new WaiterParameters<>(new DescribeStacksRequest().withStackName(stackName)), handler);

        do {
            try {
                DateTime now = DateTime.now(DateTimeZone.UTC).minusSeconds(10);
                Set<String> recordedStackEvents = Sets.newHashSet();

                TimeUnit.SECONDS.sleep(5);

                StackStatus stackStatus = getStackStatus(stackName);

                if (stackStatus != null) {
                    List<StackEvent> stackEvents = getStackEvents(stackName);
                    stackEvents.sort(Comparator.comparing(StackEvent::getTimestamp));

                    for (StackEvent stackEvent : stackEvents) {
                        DateTime eventTime = new DateTime(stackEvent.getTimestamp());
                        if (!recordedStackEvents.contains(stackEvent.getEventId()) && now.isBefore(eventTime)) {
                            logger.info(
                                    String.format("TS: %s, Status: %s, Type: %s, Reason: %s",
                                            Chalk.on(stackEvent.getTimestamp().toString()).yellow(),
                                            getStatusColor(stackEvent.getResourceStatus()),
                                            Chalk.on(stackEvent.getResourceType()).yellow(),
                                            Chalk.on(stackEvent.getResourceStatusReason()).yellow()));

                            recordedStackEvents.add(stackEvent.getEventId());
                        }
                    }
                }
            } catch (InterruptedException e) {
                logger.error("Polling interrupted", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("Failed to poll and print stack", e);
            }
        } while (!future.isDone());

        if (!handler.wasSuccess) {
            throw new UnexpectedCloudFormationStatusException(
                    String.format("Failed to create stack: %s, msg: %s", stackName, handler.getErrorMessage()));
        }
    }

    /**
     * Updates an existing stack
     */
    public void updateStackAndWait(final Stack stack,
                                   final Map<String, String> parameters,
                                   final boolean iamCapabilities,
                                   final boolean overwrite,
                                   final Map<String, String> globalTags) {

        String stackName = stack.getFullName(environmentMetadata.getName());
        final UpdateStackRequest request = new UpdateStackRequest()
                .withStackName(stackName)
                .withParameters(convertParameters(parameters));

        if (overwrite) {
            request.withTemplateBody(stack.getTemplateText());
        } else {
            request.withUsePreviousTemplate(true);
        }

        if (iamCapabilities) {
            request.getCapabilities().add("CAPABILITY_IAM");
        }

        request.setTags(getTags(globalTags));

        cloudFormationClient.updateStack(request);

        waitAndPrintCFEvents(stackName, waiters.stackUpdateComplete());

    }

    /**
     * Deletes an existing stack by name.
     *
     * @param stackName Stack ID.
     */
    public void deleteStackAndWait(final String stackName) {
        final DeleteStackRequest request = new DeleteStackRequest().withStackName(stackName);
        cloudFormationClient.deleteStack(request);
        waitAndPrintCFEvents(stackName, waiters.stackDeleteComplete());
    }

    /**
     * Returns the current status of the named stack.
     *
     * @param stackName Stack ID.
     * @return Stack status data.
     */
    @Nullable
    public StackStatus getStackStatus(final String stackName) {
        final DescribeStacksRequest request = new DescribeStacksRequest().withStackName(stackName);

        try {
            final DescribeStacksResult result = cloudFormationClient.describeStacks(request);

            if (result.getStacks().size() > 0) {
                final String status = result.getStacks().get(0).getStackStatus();

                if (StringUtils.isNotBlank(status)) {
                    return StackStatus.fromValue(status);
                }
            }
        } catch (final AmazonServiceException ase) {
            // Stack doesn't exist, just return with no status
            if (ase.getStatusCode() != 400) {
                throw ase;
            }
        }

        return null;
    }

    /**
     * Returns the current status of the named stack.
     *
     * @param stackName Stack name.
     * @return Stack outputs data.
     */
    public Map<String, String> getStackParameters(final String stackName) {
        final DescribeStacksRequest request = new DescribeStacksRequest().withStackName(stackName);
        final DescribeStacksResult result = cloudFormationClient.describeStacks(request);
        final Map<String, String> parameters = Maps.newHashMap();

        if (result.getStacks().size() > 0) {
            parameters.putAll(result.getStacks().get(0).getParameters().stream().collect(
                    Collectors.toMap(Parameter::getParameterKey, Parameter::getParameterValue)));

        }

        return parameters;
    }

    /**
     * Returns the current status of the named stack.
     *
     * @param stackName Stack name.
     * @return Stack outputs data.
     */
    public Map<String, String> getStackOutputs(final String stackName) {
        final DescribeStacksRequest request = new DescribeStacksRequest().withStackName(stackName);
        final DescribeStacksResult result = cloudFormationClient.describeStacks(request);
        final Map<String, String> outputs = Maps.newHashMap();

        if (result.getStacks().size() > 0) {
            outputs.putAll(result.getStacks().get(0).getOutputs().stream().collect(
                    Collectors.toMap(Output::getOutputKey, Output::getOutputValue)));

        }

        return outputs;
    }

    public List<StackResourceSummary> getStackResources(String stackName) {
        List<StackResourceSummary> stackResourceSummaries = new LinkedList<>();
        ListStackResourcesResult result;
        do {
            result = cloudFormationClient.listStackResources(
                    new ListStackResourcesRequest().withStackName(stackName)
            );
            stackResourceSummaries.addAll(result.getStackResourceSummaries());
        } while (result.getNextToken() != null);

        return stackResourceSummaries;
    }

    /**
     * Returns the events for a named stack.
     *
     * @param stackName Stack ID.
     * @return Collection of events
     */
    public List<StackEvent> getStackEvents(final String stackName) {
        final DescribeStackEventsRequest request = new DescribeStackEventsRequest().withStackName(stackName);

        try {
            final DescribeStackEventsResult result = cloudFormationClient.describeStackEvents(request);
            return result.getStackEvents();
        } catch (final AmazonServiceException ase) {
            // Stack doesn't exist, just return with no status
            if (ase.getStatusCode() != 400) {
                throw ase;
            }
        }

        return Collections.emptyList();
    }

    /**
     * Get the full ID of the stack
     *
     * @param stackName - The stack logical id / full stack name
     * @return
     */
    public String getStackId(String stackName) {
        Preconditions.checkArgument(StringUtils.isNotBlank(stackName), "Stack name cannot be blank");

        DescribeStacksResult result = cloudFormationClient.describeStacks(
                new DescribeStacksRequest()
                        .withStackName(stackName));

        List<com.amazonaws.services.cloudformation.model.Stack> stacks = result.getStacks();
        if (stacks.isEmpty()) {
            throw new IllegalArgumentException("No stack found with name: " + stackName);
        } else if (stacks.size() > 1) {
            logger.warn("Found more than stack with name: {}. Selecting the first one: stack id: {}",
                    stackName,
                    stacks.get(0).getStackId());
        }

        return stacks.get(0).getStackId();
    }

    /**
     * Checks if a stack exists with the specified ID.
     *
     * @param stackName Stack ID.
     * @return boolean
     */
    public boolean isStackPresent(final String stackName) {
        Preconditions.checkArgument(StringUtils.isNotBlank(stackName), "Stack ID cannot be blank");

        return getStackStatus(stackName) != null;
    }

    /**
     * Converts a Map into a list of {@link com.amazonaws.services.cloudformation.model.Parameter} objects.
     *
     * @param parameterMap Map to be converted.
     * @return Collection of parameters.
     */
    public Collection<Parameter> convertParameters(final Map<String, String> parameterMap) {
        final List<Parameter> parameterList = Lists.newArrayListWithCapacity(parameterMap.size());

        for (Map.Entry<String, String> entry : parameterMap.entrySet()) {
            final Parameter param = new Parameter()
                    .withParameterKey(entry.getKey())
                    .withParameterValue(entry.getValue());
            parameterList.add(param);
        }

        return parameterList;
    }


    /**
     * Since there doesn't appear to be a first class way through the SDK at this time to get a CF export. We can
     * iterate through the stacks for a given output key and return the value.
     *
     * @param outputKey The exported CF variable to search and retrieve the value of.
     * @return The value for the export if found
     */
    public Optional<String> searchStacksForOutput(String outputKey) {
        DescribeStacksResult describeStacksResult = null;
        do {
            DescribeStacksRequest request = new DescribeStacksRequest();
            if (describeStacksResult != null && describeStacksResult.getNextToken() != null) {
                request.withNextToken(describeStacksResult.getNextToken());
            }
            describeStacksResult = cloudFormationClient.describeStacks();
            for (com.amazonaws.services.cloudformation.model.Stack stack : describeStacksResult.getStacks()) {
                for (Output output : stack.getOutputs()) {
                    if (StringUtils.equals(output.getOutputKey(), outputKey)) {
                        return Optional.of(output.getOutputValue());
                    }
                }
            }

        } while (describeStacksResult.getNextToken() != null);

        return Optional.empty();
    }

    /**
     * Takes a map of key values and converts them to a collection of tags adding more global tags
     *
     * @param globalTags a map of user supplied global tags
     * @return a collection of user and global tags
     */
    private Collection<Tag> getTags(Map<String, String> globalTags) {
        Set<Tag> tags = new HashSet<>();
        globalTags.forEach((k, v) -> {
            tags.add(new Tag().withKey(k).withValue(v));
        });

        tags.add(new Tag()
                .withKey("Name")
                .withValue(ConfigConstants.ENV_PREFIX + environmentMetadata.getName())
        );

        return tags;
    }

    private String getStatusColor(String status) {
        if (status.endsWith("PROGRESS")) {
            return Chalk.on(status).yellow().toString();
        } else if (status.endsWith("COMPLETE")) {
            return Chalk.on(status).green().bold().toString();
        } else if (status.endsWith("FAILED")) {
            return Chalk.on(status).red().bold().toString();
        }
        return status;
    }

    /**
     * Waiter Handler that keeps track of status
     */
    private static class SuccessTrackingWaiterHandler extends WaiterHandler {

        boolean wasSuccess = false;

        Exception e;

        @Override
        public void onWaitSuccess(AmazonWebServiceRequest request) {
            wasSuccess = true;
        }

        @Override
        public void onWaitFailure(Exception e) {
            this.e = e;
            wasSuccess = false;
        }

        public Object getErrorMessage() {
            return e == null ? "unknown" : e.getMessage();
        }
    }
}
