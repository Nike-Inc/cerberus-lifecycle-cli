/*
 * Copyright (c) 2017 Nike, Inc.
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

import com.amazonaws.services.sns.AmazonSNS;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.nike.cerberus.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

/**
 * A Service for sending messages to the Cerberus Metrics SNS Topic, if the topic has been created.
 */
public class MetricsService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AmazonSNS sns;
    private final Optional<String> metricsTopicArn;
    private final ObjectMapper objectMapper;

    @Inject
    public MetricsService(AmazonSNS sns, ObjectMapper objectMapper, ConfigStore configStore) {
        this.sns = sns;
        this.objectMapper = objectMapper;

        metricsTopicArn = configStore.getMetricsTopicArn();
    }

    public void trackMetric(MetricType metricType, String key, Integer value, Map<String, String> dimensions) {

        log.info("Attempting to track metric, TYPE: {}, KEY: {}, VALUE: {}, DIMENSIONS: {}",
                metricType, key, value, dimensions);

        if (! metricsTopicArn.isPresent()) {
            log.warn("No metrics topic arn set, CLI will not track metric");
            return;
        }

        CerberusMetricMessage message = new CerberusMetricMessage()
                .setMetricType(metricType.toString())
                .setMetricKey(key)
                .setMetricValue(value)
                .setDimensions(dimensions);

        try {
            sns.publish(metricsTopicArn.get(), objectMapper.writeValueAsString(message));
        } catch (Throwable t) {
            log.error("Failed to track metric", t);
        }
    }

    public void trackGauge(String key, Integer value, Map<String, String> dimensions) {
        trackMetric(MetricType.GAUGE, key, value, dimensions);
    }

    public void trackCounter(String key, Integer value, Map<String, String> dimensions) {
        trackMetric(MetricType.COUNTER, key, value, dimensions);
    }

    enum MetricType {
        GAUGE("gauge"),
        COUNTER("counter");

        private final String stringValue;

        MetricType(String stringValue) {
            this.stringValue = stringValue;
        }

        @Override
        public String toString() {
            return stringValue;
        }
    }

    private static class CerberusMetricMessage {
        private String metricKey;
        private Integer metricValue;
        private String metricType;
        private Map<String, String> dimensions;

        public String getMetricKey() {
            return metricKey;
        }

        public CerberusMetricMessage setMetricKey(String metricKey) {
            this.metricKey = metricKey;
            return this;
        }

        public Integer getMetricValue() {
            return metricValue;
        }

        public CerberusMetricMessage setMetricValue(Integer metricValue) {
            this.metricValue = metricValue;
            return this;
        }

        public String getMetricType() {
            return metricType;
        }

        public CerberusMetricMessage setMetricType(String metricType) {
            this.metricType = metricType;
            return this;
        }

        public Map<String, String> getDimensions() {
            return dimensions;
        }

        public CerberusMetricMessage setDimensions(Map<String, String> dimensions) {
            this.dimensions = dimensions;
            return this;
        }
    }

}
