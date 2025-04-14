package com.example.jobscheduler.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the application.
 * Provides consistent error responses for various exceptions.
 * Ensures that the application returns meaningful error messages to the client.
 */
@ControllerAdvice // Indicates that this class provides centralized exception handling for controllers.
@Slf4j // Lombok annotation to enable logging in this class.
public class GlobalExceptionHandler {

    /**
     * Handles validation exceptions thrown when request body validation fails.
     * Extracts validation errors and returns them in a structured response.
     *
     * @param ex the MethodArgumentNotValidException
     * @return a response entity with validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>(); // Response map to hold error details.
        Map<String, String> errors = new HashMap<>(); // Map to store field-specific validation errors.

        // Extract validation errors from the exception.
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField(); // Field name where validation failed.
            String errorMessage = error.getDefaultMessage(); // Validation error message.
            errors.put(fieldName, errorMessage); // Add the field and error message to the map.
        });

        // Populate the response with error details.
        response.put("timestamp", LocalDateTime.now()); // Current timestamp.
        response.put("status", HttpStatus.BAD_REQUEST.value()); // HTTP status code for bad request.
        response.put("error", "Validation Error"); // Error type.
        response.put("details", errors); // Field-specific validation errors.

        log.warn("Validation error: {}", errors); // Log the validation errors.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // Return the response with HTTP 400 status.
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
        Map<String, Object> response = new HashMap<>(); // Response map to hold error details.

        // Populate the response with error details.
        response.put("timestamp", LocalDateTime.now()); // Current timestamp.
        response.put("status", HttpStatus.BAD_REQUEST.value()); // HTTP status code for bad request.
        response.put("error", "Bad Request"); // Error type.
        response.put("message", ex.getMessage()); // Exception message.

        log.warn("Bad request: {}", ex.getMessage()); // Log the exception message.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // Return the response with HTTP 400 status.
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
        Map<String, Object> response = new HashMap<>(); // Response map to hold error details.

        // Populate the response with error details.
        response.put("timestamp", LocalDateTime.now()); // Current timestamp.
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value()); // HTTP status code for internal server error.
        response.put("error", "Internal Server Error"); // Error type.
        response.put("message", "An unexpected error occurred"); // Generic error message.

        log.error("Unexpected error: {}", ex.getMessage(), ex); // Log the exception details.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // Return the response with HTTP 500 status.
    }
}