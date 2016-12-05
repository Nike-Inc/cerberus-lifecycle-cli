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

public class CloudFrontLogProcessingLambdaConfig {

    private String manualWhiteListIpSetId;

    private String manualBlackListIpSetId;

    private String rateLimitAutoBlackListIpSetId;

    private Integer rateLimitViolationBlacklistPeriodInMinutes;

    private Integer requestPerMinuteLimit;

    private String slackWebHookUrl;

    private String slackIcon;

    private String googleAnalyticsId;

    public String getManualWhiteListIpSetId() {
        return manualWhiteListIpSetId;
    }

    public void setManualWhiteListIpSetId(String manualWhiteListIpSetId) {
        this.manualWhiteListIpSetId = manualWhiteListIpSetId;
    }

    public String getManualBlackListIpSetId() {
        return manualBlackListIpSetId;
    }

    public void setManualBlackListIpSetId(String manualBlackListIpSetId) {
        this.manualBlackListIpSetId = manualBlackListIpSetId;
    }

    public String getRateLimitAutoBlackListIpSetId() {
        return rateLimitAutoBlackListIpSetId;
    }

    public void setRateLimitAutoBlackListIpSetId(String rateLimitAutoBlackListIpSetId) {
        this.rateLimitAutoBlackListIpSetId = rateLimitAutoBlackListIpSetId;
    }

    public Integer getRateLimitViolationBlacklistPeriodInMinutes() {
        return rateLimitViolationBlacklistPeriodInMinutes;
    }

    public void setRateLimitViolationBlacklistPeriodInMinutes(Integer rateLimitViolationBlacklistPeriodInMinutes) {
        this.rateLimitViolationBlacklistPeriodInMinutes = rateLimitViolationBlacklistPeriodInMinutes;
    }

    public Integer getRequestPerMinuteLimit() {
        return requestPerMinuteLimit;
    }

    public void setRequestPerMinuteLimit(Integer requestPerMinuteLimit) {
        this.requestPerMinuteLimit = requestPerMinuteLimit;
    }

    public String getSlackWebHookUrl() {
        return slackWebHookUrl;
    }

    public void setSlackWebHookUrl(String slackWebHookUrl) {
        this.slackWebHookUrl = slackWebHookUrl;
    }

    public String getSlackIcon() {
        return slackIcon;
    }

    public void setSlackIcon(String slackIcon) {
        this.slackIcon = slackIcon;
    }

    public String getGoogleAnalyticsId() {
        return googleAnalyticsId;
    }

    public void setGoogleAnalyticsId(String googleAnalyticsId) {
        this.googleAnalyticsId = googleAnalyticsId;
    }
}
