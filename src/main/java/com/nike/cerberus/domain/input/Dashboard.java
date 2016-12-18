package com.nike.cerberus.domain.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Dashboard {

    private String artifactUrl;
    private String overrideArtifactUrl;

    public String getArtifactUrl() {
        return artifactUrl;
    }

    public void setArtifactUrl(String artifactUrl) {
        this.artifactUrl = artifactUrl;
    }

    public String getOverrideArtifactUrl() {
        return overrideArtifactUrl;
    }

    public void setOverrideArtifactUrl(String overrideArtifactUrl) {
        this.overrideArtifactUrl = overrideArtifactUrl;
    }
}
