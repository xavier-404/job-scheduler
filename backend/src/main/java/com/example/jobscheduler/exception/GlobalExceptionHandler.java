package com.example.jobscheduler.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the application.
 * Provides consistent error responses for various exceptions.
 * Ensures that the application returns meaningful error messages to the client.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles resource not found exceptions like favicon.ico requests
     * 
     * @param ex the NoResourceFoundException
     * @return a response entity with 404 status code
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFoundException(NoResourceFoundException ex) {
        // For favicon.ico requests, don't log an error as this is normal browser behavior
        if (ex.getMessage().contains("favicon.ico")) {
            // Just return 404 without logging
            return ResponseEntity.notFound().build();
        }
        
        // For other resources, log at debug level and return 404
        log.debug("Resource not found: {}", ex.getMessage());
        return ResponseEntity.notFound().build();
    }

    /**
     * Handles validation exceptions thrown when request body validation fails.
     * Extracts validation errors and returns them in a structured response.
     *
     * @param ex the MethodArgumentNotValidException
     * @return a response entity with validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();

        // Extract validation errors from the exception
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        // Populate the response with error details
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Error");
        response.put("details", errors);

        log.warn("Validation error: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles InvalidTimeZoneException thrown when an invalid timezone is provided.
     *
     * @param ex the InvalidTimeZoneException
     * @return a response entity with the error message
     */
    @ExceptionHandler(InvalidTimeZoneException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTimeZoneException(InvalidTimeZoneException ex) {
        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Invalid Time Zone");
        response.put("message", ex.getMessage());

        log.warn("Invalid time zone: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles PastScheduleTimeException thrown when a job is scheduled for a time in the past.
     *
     * @param ex the PastScheduleTimeException
     * @return a response entity with the error message
     */
    @ExceptionHandler(PastScheduleTimeException.class)
    public ResponseEntity<Map<String, Object>> handlePastScheduleTimeException(PastScheduleTimeException ex) {
        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Invalid Schedule Time");
        response.put("message", ex.getMessage());

        log.warn("Past schedule time: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Handles AsyncJobSchedulingException thrown when an asynchronous job scheduling operation fails.
     *
     * @param ex the AsyncJobSchedulingException
     * @return a response entity with the error message
     */
    @ExceptionHandler(AsyncJobSchedulingException.class)
    public ResponseEntity<Map<String, Object>> handleAsyncJobSchedulingException(AsyncJobSchedulingException ex) {
        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "Job Scheduling Error");
        response.put("message", ex.getMessage());

        log.error("Async job scheduling error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handles IllegalArgumentException thrown when invalid arguments are passed.
     * Returns a structured error response with the exception message.
     *
     * @param ex the IllegalArgumentException
     * @return a response entity with the error message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Bad Request");
        response.put("message", ex.getMessage());

        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles all other exceptions that are not explicitly handled.
     * Returns a generic error response for unexpected errors.
     *
     * @param ex the Exception
     * @return a response entity with the error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "Internal Server Error");
        response.put("message", "An unexpected error occurred");
        
        // Include exception details in the response for non-production environments
        // In production, consider removing this to avoid exposing implementation details
        if (System.getProperty("spring.profiles.active", "").contains("dev")) {
            response.put("exception", ex.getClass().getName());
            response.put("details", ex.getMessage());
        }

        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}