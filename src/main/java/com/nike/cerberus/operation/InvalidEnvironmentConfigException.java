package com.nike.cerberus.operation;

/**
 * Represents attempting to use environment config that isn't in the expected state.
 */
public class InvalidEnvironmentConfigException extends RuntimeException {
    public InvalidEnvironmentConfigException(final String errorMessage) {
        super(errorMessage);
    }
}
