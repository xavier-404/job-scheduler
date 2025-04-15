package com.example.jobscheduler.exception;

/**
 * Exception thrown when an asynchronous job scheduling operation fails.
 */
public class AsyncJobSchedulingException extends RuntimeException {
    public AsyncJobSchedulingException(String message, Throwable cause) {
        super(message, cause);
    }

    public AsyncJobSchedulingException(String message) {
        super(message);
    }
}