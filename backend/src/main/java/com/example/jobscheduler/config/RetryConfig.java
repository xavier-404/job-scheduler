package com.example.jobscheduler.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Configuration to enable Spring Retry functionality.
 * This allows methods annotated with @Retryable to automatically retry on failure.
 */
@Configuration
@EnableRetry
public class RetryConfig {
    // No additional configuration needed here; @EnableRetry activates Spring Retry
}