package com.example.jobscheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Job Scheduler service.
 * This Spring Boot application manages the scheduling and execution of jobs
 * that fetch client-specific data and publish it to Kafka.
 */
@SpringBootApplication
public class JobSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobSchedulerApplication.class, args);
    }
}