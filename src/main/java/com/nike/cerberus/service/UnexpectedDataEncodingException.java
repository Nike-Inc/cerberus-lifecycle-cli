package com.nike.cerberus.service;

/**
 * File contents are not UTF-8 encoded.
 */
public class UnexpectedDataEncodingException extends RuntimeException {
    public UnexpectedDataEncodingException(String message, Throwable e) {
        super(message, e);
    }
}
