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
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.beust.jcommander.internal.Maps;
import com.github.tomaslanger.chalk.Chalk;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.domain.EnvironmentMetadata;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service wrapper for AWS CloudFormation.
 */
public class CloudFormationService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AmazonCloudFormation cloudFormationClient;
    private final EnvironmentMetadata environmentMetadata;

    @Inject
    public CloudFormationService(final AmazonCloudFormation cloudFormationClient,
                                 final EnvironmentMetadata environmentMetadata) {
        this.cloudFormationClient = cloudFormationClient;
        this.environmentMetadata = environmentMetadata;
    }

    /**
     * Creates a new stack.
     *
     * @param name Stack name.
     * @param parameters Input parameters.
     * @param templatePath Classpath to the JSON template of the stack.
     * @return Stack ID
     */
    public String createStack(final String name,
                              final Map<String, String> parameters,
                              final String templatePath,
                              final boolean iamCapabilities) {
        logger.info(String.format("Executing the Cloud Formation: %s, Stack Name: %s", templatePath, name));

        final CreateStackRequest request = new CreateStackRequest()
                .withStackName(name)
                .withParameters(convertParameters(parameters))
                .withTemplateBody(getTemplateText(templatePath));

        if (iamCapabilities) {
            request.getCapabilities().add("CAPABILITY_IAM");
        }

        final CreateStackResult result = cloudFormationClient.createStack(request);
        return result.getStackId();
    }

    /**
     * Updates an existing stack by name.
     *
     * @param stackId
     * @param parameters
     * @param iamCapabilities
     */
    public void updateStack(final String stackId,
                            final Map<String, String> parameters,
                            final boolean iamCapabilities) {
        updateStack(stackId, parameters, null, iamCapabilities);
    }

    /**
     * Updates an existing stack by name.
     *
     * @param stackId Stack ID.
     * @param parameters Input parameters.
     * @param templatePath Path to the JSON template of the stack.
     */
    public void updateStack(final String stackId,
                            final Map<String, String> parameters,
                            final String templatePath,
                            final boolean iamCapabilities) {
        final UpdateStackRequest request = new UpdateStackRequest()
                .withStackName(stackId)
                .withParameters(convertParameters(parameters));

        if (StringUtils.isNotBlank(templatePath)) {
            request.withTemplateBody(getTemplateText(templatePath));
        } else {
            request.withUsePreviousTemplate(true);
        }

        if (iamCapabilities) {
            request.getCapabilities().add("CAPABILITY_IAM");
        }

        cloudFormationClient.updateStack(request);
    }

    /**
     * Deletes an existing stack by name.
     *
     * @param stackId Stack ID.
     */
    public void deleteStack(final String stackId) {
        final DeleteStackRequest request = new DeleteStackRequest().withStackName(stackId);
        cloudFormationClient.deleteStack(request);
    }

    /**
     * Returns the current status of the named stack.
     *
     * @param stackId Stack ID.
     * @return Stack status data.
     */
    @Nullable
    public StackStatus getStackStatus(final String stackId) {
        final DescribeStacksRequest request = new DescribeStacksRequest().withStackName(stackId);

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
     * @param stackId Stack name.
     * @return Stack outputs data.
     */
    public Map<String, String> getStackParameters(final String stackId) {
        final DescribeStacksRequest request = new DescribeStacksRequest().withStackName(stackId);
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
     * @param stackId Stack name.
     * @return Stack outputs data.
     */
    public Map<String, String> getStackOutputs(final String stackId) {
        final DescribeStacksRequest request = new DescribeStacksRequest().withStackName(stackId);
        final DescribeStacksResult result = cloudFormationClient.describeStacks(request);
        final Map<String, String> outputs = Maps.newHashMap();

        if (result.getStacks().size() > 0) {
            outputs.putAll(result.getStacks().get(0).getOutputs().stream().collect(
                    Collectors.toMap(Output::getOutputKey, Output::getOutputValue)));

        }

        return outputs;
    }

    /**
     * Returns the events for a named stack.
     *
     * @param stackId Stack ID.
     * @return Collection of events
     */
    public List<StackEvent> getStackEvents(final String stackId) {
        final DescribeStackEventsRequest request = new DescribeStackEventsRequest().withStackName(stackId);

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
     * Checks if a stack exists with the specified ID.
     *
     * @param stackId Stack ID.
     * @return boolean
     */
    public boolean isStackPresent(final String stackId) {
        Preconditions.checkArgument(StringUtils.isNotBlank(stackId), "Stack ID can not be blank");

        return getStackStatus(stackId) != null;
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
     * Gets the template contents from the file on the classpath.
     *
     * @param templatePath Classpath for the template to be read
     * @return Template contents
     */
    public String getTemplateText(final String templatePath) {
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
     * Prepends the name with the environment name. ([environment]-[name])
     *
     * @param name Custom stack name
     * @return Formatted name with environment prepended
     */
    public String getEnvStackName(final String name) {
        return String.format("%s-%s", environmentMetadata.getName(), name);
    }

    /**
     * Blocking call that waits for a stack change to complete.  Times out if waiting more than 30 minutes.
     *
     * @param endStatuses Status to end on
     * @return The final status
     */
    public StackStatus waitForStatus(final String stackId, final HashSet<StackStatus> endStatuses) {
        DateTime now = DateTime.now(DateTimeZone.UTC).minusSeconds(10);
        DateTime timeoutDateTime = now.plusMinutes(90);
        Set<String> recordedStackEvents = Sets.newHashSet();
        boolean isRunning = true;
        StackStatus stackStatus = null;

        while (isRunning) {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                logger.warn("Thread sleep interrupted. Continuing...", e);
            }
            stackStatus = getStackStatus(stackId);

            if (endStatuses.contains(stackStatus) || stackStatus == null) {
                isRunning = false;
            }

            if (stackStatus != null) {
                List<StackEvent> stackEvents = getStackEvents(stackId);
                stackEvents.sort((o1, o2) -> o1.getTimestamp().compareTo(o2.getTimestamp()));

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

            if (timeoutDateTime.isBeforeNow()) {
                logger.error("Timed out waiting for CloudFormation completion status.");
                isRunning = false;
            }
        }

        return stackStatus;
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
}
