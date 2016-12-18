package com.nike.cerberus.domain.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EdgeSecurity {

    private String cloudfrontLambdaArtifactUrl;
    private String cloudfrontSecurityGroupIpSyncLambdaArtifactUrl;
    private String rateLimitPerMinute;
    private String rateLimitViolationBlockPeriodInMinutes;
    private String googleAnalyticsTrackingId;
    private String slackWebHookUrl;
    private String slackIcon;

    public String getCloudfrontLambdaArtifactUrl() {
        return cloudfrontLambdaArtifactUrl;
    }

    public void setCloudfrontLambdaArtifactUrl(String cloudfrontLambdaArtifactUrl) {
        this.cloudfrontLambdaArtifactUrl = cloudfrontLambdaArtifactUrl;
    }

    public String getCloudfrontSecurityGroupIpSyncLambdaArtifactUrl() {
        return cloudfrontSecurityGroupIpSyncLambdaArtifactUrl;
    }

    public void setCloudfrontSecurityGroupIpSyncLambdaArtifactUrl(String cloudfrontSecurityGroupIpSyncLambdaArtifactUrl) {
        this.cloudfrontSecurityGroupIpSyncLambdaArtifactUrl = cloudfrontSecurityGroupIpSyncLambdaArtifactUrl;
    }

    public String getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(String rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public String getRateLimitViolationBlockPeriodInMinutes() {
        return rateLimitViolationBlockPeriodInMinutes;
    }

    public void setRateLimitViolationBlockPeriodInMinutes(String rateLimitViolationBlockPeriodInMinutes) {
        this.rateLimitViolationBlockPeriodInMinutes = rateLimitViolationBlockPeriodInMinutes;
    }

    public String getGoogleAnalyticsTrackingId() {
        return googleAnalyticsTrackingId;
    }

    public void setGoogleAnalyticsTrackingId(String googleAnalyticsTrackingId) {
        this.googleAnalyticsTrackingId = googleAnalyticsTrackingId;
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
}
