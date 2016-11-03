package com.nike.cerberus.domain.cloudformation;

/**
 * Interface implemented by stack parameter POJOs that have tag parameters.
 */
public interface TagParameters {

    TagParametersDelegate getTagParameters();
}
