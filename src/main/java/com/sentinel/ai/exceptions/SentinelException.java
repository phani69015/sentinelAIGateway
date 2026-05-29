package com.sentinel.ai.exceptions;

public class SentinelException extends RuntimeException {

    public SentinelException(String message) {
        super(message);
    }

    public SentinelException(String message, Throwable cause) {
        super(message, cause);
    }
}
