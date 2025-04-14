package com.example.jobscheduler.dto;

import com.example.jobscheduler.model.Job;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO (Data Transfer Object) for job creation requests from the client.
 * Contains all necessary information to schedule a new job.
 */
@Data // Lombok annotation to generate getters, setters, toString, equals, and hashCode methods.
@Builder // Lombok annotation to implement the builder pattern for this class.
@NoArgsConstructor // Lombok annotation to generate a no-argument constructor.
@AllArgsConstructor // Lombok annotation to generate an all-argument constructor.
public class JobRequest {

    @NotBlank(message = "Client ID is required") // Validates that the clientId field is not null or blank.
    private String clientId; // Identifier for the client associated with the job.

    @NotNull(message = "Schedule type is required") // Validates that the scheduleType field is not null.
    private Job.ScheduleType scheduleType; // Type of schedule for the job (e.g., IMMEDIATE, ONE_TIME, RECURRING).

    // For ONE_TIME jobs
    private LocalDateTime startTime; // Start time for the job (applicable for ONE_TIME jobs).

    // For RECURRING jobs
    private String cronExpression; // Cron expression defining the schedule for recurring jobs.

    // Time zone for the job
    private String timeZone; // Time zone in which the job will be executed.

    // For WEEKLY recurring jobs - days of the week (1-7 for Monday-Sunday)
    private List<Integer> daysOfWeek; // List of days of the week for weekly recurring jobs.

    // For MONTHLY recurring jobs - days of the month (1-31)
    private List<Integer> daysOfMonth; // List of days of the month for monthly recurring jobs.

    // For HOURLY recurring jobs - hours interval
    private Integer hourlyInterval; // Interval in hours for hourly recurring jobs.

    // Additional fields for recurring jobs
    private Integer recurringTimeHour; // Hour of the day (0-23) for recurring jobs.
    private Integer recurringTimeMinute; // Minute of the hour (0-59) for recurring jobs.
}