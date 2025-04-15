package com.example.jobscheduler.exception;

/**
 * Exception thrown when a job is scheduled for a time in the past.
 */
public class PastScheduleTimeException extends RuntimeException {
    public PastScheduleTimeException(String message) {
        super(message);
    }
}
