package com.nike.cerberus.generator;

/**
 * Error thrown when config generation fails.
 */
public class ConfigGenerationException extends RuntimeException {
    public ConfigGenerationException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
