package com.example.jobscheduler.exception;

/**
 * Exception thrown when an invalid timezone is provided.
 */
public class InvalidTimeZoneException extends RuntimeException {
    public InvalidTimeZoneException(String message) {
        super(message);
    }
}