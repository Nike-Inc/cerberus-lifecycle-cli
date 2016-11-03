package com.nike.cerberus.operation;

/**
 * Represents an unexpected CloudFormation status.
 */
public class UnexpectedCloudFormationStatusException extends RuntimeException {
    public UnexpectedCloudFormationStatusException(final String errorMessage) {
        super(errorMessage);
    }
}
