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

package com.nike.cerberus.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
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
import com.amazonaws.services.cloudformation.model.TemplateParameter;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.amazonaws.services.cloudformation.model.ValidateTemplateRequest;
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
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.UnexpectedCloudFormationStatusException;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
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

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

/**
 * Service wrapper for AWS CloudFormation.
 */
public class CloudFormationService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public final static String AUTO_SCALING_GROUP_LOGICAL_ID_OUTPUT_KEY = "autoscalingGroupLogicalId";

    public final static String MIN_INSTANCES_STACK_PARAMETER_KEY = "minimumInstances";

    private final AwsClientFactory<AmazonCloudFormationClient> cloudFormationClientFactory;

    private final String environmentName;

    @Inject
    public CloudFormationService(AwsClientFactory<AmazonCloudFormationClient> cloudFormationClientFactory,
                                 @Named(ENV_NAME) String environmentName) {

        this.cloudFormationClientFactory = cloudFormationClientFactory;
        this.environmentName = environmentName;
    }

    /**
     * Creates a new stack in the provided region.
     *
     * @param region The region for the stack
     * @param stack the stack that is being updated
     * @param parameters the parameters to send to the cloud formation
     * @param iamCapabilities flag for iam capabilities
     * @param globalTags map of tags to apply to all resources created/updated
     * @return Stack ID
     */
    public String createStackAndWait(Regions region,
                                     Stack stack,
                                     Map<String, String> parameters,
                                     boolean iamCapabilities,
                                     Map<String, String> globalTags) {

        String stackName = stack.getFullName(environmentName);

        log.info("Creating: Cloud Formation Template: {}, Stack Name: {}, Region: {}", stack.getTemplatePath(), stackName, region.getName());

        CreateStackRequest request = new CreateStackRequest()
                .withStackName(stack.getFullName(environmentName))
                .withParameters(convertParameters(parameters))
                .withTemplateBody(stack.getTemplateText())
                .withTags(getTags(globalTags));

        if (iamCapabilities) {
            request.getCapabilities().add("CAPABILITY_IAM");
        }

        AmazonCloudFormation cloudFormationClient = cloudFormationClientFactory.getClient(region);
        CreateStackResult result = cloudFormationClient.createStack(request);
        waitAndPrintCFEvents(region, stackName, new AmazonCloudFormationWaiters(cloudFormationClient).stackCreateComplete());

        return result.getStackId();
    }

    /**
     * Uses AWS CF Aync Waiters to wait for Cloud Formation actions to complete, while logging events and verifying success
     *
     * @param region The Region to use
     * @param stackName The stack that is having an action performed
     * @param waiter The Amazon waiter
     */
    private void waitAndPrintCFEvents(Regions region, String stackName, Waiter waiter) {
        SuccessTrackingWaiterHandler handler = new SuccessTrackingWaiterHandler();

        @SuppressWarnings("unchecked")
        Future future = waiter
                .runAsync(new WaiterParameters<>(new DescribeStacksRequest().withStackName(stackName)), handler);

        Set<String> recordedStackEvents = Sets.newHashSet();
        do {
            try {
                DateTime now = DateTime.now(DateTimeZone.UTC).minusSeconds(10);

                TimeUnit.SECONDS.sleep(5);

                StackStatus stackStatus = getStackStatus(region, stackName);
                if (stackStatus != null) {
                    List<StackEvent> stackEvents = getStackEvents(region, stackName);
                    stackEvents.sort(Comparator.comparing(StackEvent::getTimestamp));

                    for (StackEvent stackEvent : stackEvents) {
                        DateTime eventTime = new DateTime(stackEvent.getTimestamp());
                        if (!recordedStackEvents.contains(stackEvent.getEventId()) && now.isBefore(eventTime)) {
                            log.info(
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
                log.error("Polling interrupted", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Failed to poll and print stack", e);
            }
        } while (!future.isDone());

        if (!handler.wasSuccess) {
            throw new UnexpectedCloudFormationStatusException(
                    String.format("Failed to create stack: %s, msg: %s", stackName, handler.getErrorMessage()));
        }
    }

    /**
     * Updates an existing stack in the provided region
     * @param region The region for the stack
     * @param stack the stack that is being updated
     * @param parameters the parameters to send to the cloud formation
     * @param iamCapabilities flag for iam capabilities
     * @param overwrite overwrite the deployed template with the current template in the cli
     * @param globalTags map of tags to apply to all resources created/updated
     */
    public void updateStackAndWait(Regions region,
                                   Stack stack,
                                   Map<String, String> parameters,
                                   boolean iamCapabilities,
                                   boolean overwrite,
                                   Map<String, String> globalTags) {

        String stackName = stack.getFullName(environmentName);

        log.info("Updating: Cloud Formation Template: {}, Stack Name: {}, Region: {}", stack.getTemplatePath(), stackName, region.getName());

        UpdateStackRequest request = new UpdateStackRequest()
                .withStackName(stackName);

        if (overwrite) {
            String template = stack.getTemplateText();
            request.withTemplateBody(template);
            // filter out params that are no longer in the template
            parameters.keySet().retainAll(validateTemplateAndRetrieveParameters(template, region));
        } else {
            request.withUsePreviousTemplate(true);
        }

        request.withParameters(convertParameters(parameters));

        if (iamCapabilities) {
            request.getCapabilities().add("CAPABILITY_IAM");
        }

        request.setTags(getTags(globalTags));

        AmazonCloudFormation cloudFormationClient = cloudFormationClientFactory.getClient(region);
        cloudFormationClient.updateStack(request);

        waitAndPrintCFEvents(region, stackName, new AmazonCloudFormationWaiters(cloudFormationClient).stackUpdateComplete());

    }

    /**
     * Validates and retrieves the set of parameter keys for a template.
     * @param templateText The template to validate
     * @param region The region that is being used
     * @return The set of parameters that a template has.
     */
    private Set<String> validateTemplateAndRetrieveParameters(String templateText, Regions region) {
        return cloudFormationClientFactory.getClient(region).validateTemplate(
                new ValidateTemplateRequest().withTemplateBody(templateText)
        ).getParameters().stream().map(TemplateParameter::getParameterKey).collect(Collectors.toSet());
    }

    /**
     * Deletes an existing stack by name in the region provided.
     *
     * @param region The Region to use
     * @param stackName Stack ID.
     */
    public void deleteStackAndWait(Regions region, String stackName) {
        log.info("Deleting the Stack Name: {}, Region: {}", stackName, region.getName());
        DeleteStackRequest request = new DeleteStackRequest().withStackName(stackName);
        AmazonCloudFormation cloudFormationClient = cloudFormationClientFactory.getClient(region);
        cloudFormationClient.deleteStack(request);
        waitAndPrintCFEvents(region, stackName, new AmazonCloudFormationWaiters(cloudFormationClient).stackDeleteComplete());
    }

    /**
     * Returns the current status of the named stack.
     *
     * @param region The Region to use
     * @param stackName Stack ID.
     * @return Stack status data.
     */
    @Nullable
    public StackStatus getStackStatus(Regions region, String stackName) {
        AmazonCloudFormation cloudFormationClient = cloudFormationClientFactory.getClient(region);
        DescribeStacksRequest request = new DescribeStacksRequest().withStackName(stackName);

        try {
            DescribeStacksResult result = cloudFormationClient.describeStacks(request);

            if (result.getStacks().size() > 0) {
                String status = result.getStacks().get(0).getStackStatus();

                if (StringUtils.isNotBlank(status)) {
                    return StackStatus.fromValue(status);
                }
            }
        } catch (AmazonServiceException ase) {
            // Stack doesn't exist, just return with no status
            if (ase.getStatusCode() != 400) {
                throw ase;
            }
        }

        return null;
    }

    /**
     * Returns the current status of the named stack in the provided region.
     *
     * @param region The Region to use
     * @param stackName Stack name.
     * @return Stack outputs data.
     */
    public Map<String, String> getStackParameters(Regions region, String stackName) {
        AmazonCloudFormation cloudFormationClient = cloudFormationClientFactory.getClient(region);
        DescribeStacksRequest request = new DescribeStacksRequest().withStackName(stackName);
        DescribeStacksResult result = cloudFormationClient.describeStacks(request);
        Map<String, String> parameters = Maps.newHashMap();

        if (result.getStacks().size() > 0) {
            parameters.putAll(result.getStacks().get(0).getParameters().stream().collect(
                    Collectors.toMap(Parameter::getParameterKey, Parameter::getParameterValue)));

        }

        return parameters;
    }

    public Map<String, String> getStackTags(Regions region, String stackName) {
        AmazonCloudFormation cloudFormationClient = cloudFormationClientFactory.getClient(region);
        DescribeStacksRequest request = new DescribeStacksRequest().withStackName(stackName);
        DescribeStacksResult result = cloudFormationClient.describeStacks(request);
        Map<String, String> tags = Maps.newHashMap();

        if (result.getStacks().size() > 0) {
            tags.putAll(result.getStacks().get(0).getTags().stream().collect(
                    Collectors.toMap(Tag::getKey, Tag::getValue)));

        }

        return tags;
    }

    /**
     * Returns the current status of the named stack.
     *
     * @param region The Region to use
     * @param stackName Stack name.
     * @return Stack outputs data.
     */
    public Map<String, String> getStackOutputs(Regions region, String stackName) {
        AmazonCloudFormation cloudFormationClient = cloudFormationClientFactory.getClient(region);
        DescribeStacksRequest request = new DescribeStacksRequest().withStackName(stackName);
        DescribeStacksResult result = cloudFormationClient.describeStacks(request);
        Map<String, String> outputs = Maps.newHashMap();

        if (result.getStacks().size() > 0) {
            outputs.putAll(result.getStacks().get(0).getOutputs().stream().collect(
                    Collectors.toMap(Output::getOutputKey, Output::getOutputValue)));

        }

        return outputs;
    }

    /**
     * Returns list of StackResourceSummary for stack provided in region provided
     *
     * @param region The region to use
     * @param stackName The stack name to use
     * @return list of StackResourceSummary
     */
    public List<StackResourceSummary> getStackResources(Regions region, String stackName) {
        AmazonCloudFormation cloudFormationClient = cloudFormationClientFactory.getClient(region);
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
     * @param region The Region to use
     * @return Collection of events
     */
    public List<StackEvent> getStackEvents(Regions region, String stackName) {
        AmazonCloudFormation cloudFormationClient = cloudFormationClientFactory.getClient(region);
        DescribeStackEventsRequest request = new DescribeStackEventsRequest().withStackName(stackName);

        try {
            DescribeStackEventsResult result = cloudFormationClient.describeStackEvents(request);
            return result.getStackEvents();
        } catch (AmazonServiceException ase) {
            // Stack doesn't exist, just return with no status
            if (ase.getStatusCode() != 400) {
                throw ase;
            }
        }

        return Collections.emptyList();
    }

    /**
     * Get stack id for stack in region provided.
     *
     * @param stackName The stack logical id / full stack name
     * @param region The Region to use
     * @return The full ID of the stack
     */
    public String getStackId(Regions region, String stackName) {
        AmazonCloudFormation cloudFormationClient = cloudFormationClientFactory.getClient(region);
        Preconditions.checkArgument(StringUtils.isNotBlank(stackName), "Stack name cannot be blank");

        DescribeStacksResult result = cloudFormationClient.describeStacks(
                new DescribeStacksRequest()
                        .withStackName(stackName));

        List<com.amazonaws.services.cloudformation.model.Stack> stacks = result.getStacks();
        if (stacks.isEmpty()) {
            throw new IllegalArgumentException("No stack found with name: " + stackName);
        } else if (stacks.size() > 1) {
            log.warn("Found more than stack with name: {}. Selecting the first one: stack id: {}",
                    stackName,
                    stacks.get(0).getStackId());
        }

        return stacks.get(0).getStackId();
    }

    /**
     * Checks if a stack exists with the specified ID.
     *
     * @param region The Region to use
     * @param stackName Stack ID.
     * @return boolean
     */
    public boolean isStackPresent(Regions region, String stackName) {
        Preconditions.checkArgument(StringUtils.isNotBlank(stackName), "Stack ID cannot be blank");

        return getStackStatus(region, stackName) != null;
    }

    /**
     * Converts a Map into a list of {@link com.amazonaws.services.cloudformation.model.Parameter} objects.
     *
     * @param parameterMap Map to be converted.
     * @return Collection of parameters.
     */
    public Collection<Parameter> convertParameters(Map<String, String> parameterMap) {
        List<Parameter> parameterList = Lists.newArrayListWithCapacity(parameterMap.size());

        for (Map.Entry<String, String> entry : parameterMap.entrySet()) {
            Parameter param = new Parameter()
                    .withParameterKey(entry.getKey())
                    .withParameterValue(entry.getValue());
            parameterList.add(param);
        }

        return parameterList;
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
                .withValue(ConfigConstants.ENV_PREFIX + environmentName)
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
