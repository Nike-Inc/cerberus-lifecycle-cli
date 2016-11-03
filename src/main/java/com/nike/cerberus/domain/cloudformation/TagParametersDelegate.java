package com.nike.cerberus.domain.cloudformation;

/**
 * CloudFormation input parameters common to all Cerberus CloudFormation stacks.
 */
public class TagParametersDelegate {

    private String tagName;

    private String tagEmail;

    private String tagCostcenter;

    public String getTagName() {
        return tagName;
    }

    public TagParametersDelegate setTagName(String tagName) {
        this.tagName = tagName;
        return this;
    }

    public String getTagEmail() {
        return tagEmail;
    }

    public TagParametersDelegate setTagEmail(String tagEmail) {
        this.tagEmail = tagEmail;
        return this;
    }

    public String getTagCostcenter() {
        return tagCostcenter;
    }

    public TagParametersDelegate setTagCostcenter(String tagCostcenter) {
        this.tagCostcenter = tagCostcenter;
        return this;
    }
}
