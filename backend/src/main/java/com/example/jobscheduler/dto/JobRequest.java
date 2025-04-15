package com.example.jobscheduler.dto;

import com.example.jobscheduler.model.Job;
import com.fasterxml.jackson.annotation.JsonFormat;
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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequest {

    @NotBlank(message = "Client ID is required")
    private String clientId;

    @NotNull(message = "Schedule type is required")
    private Job.ScheduleType scheduleType;

    // For ONE_TIME jobs
    // The format ensures the date is parsed correctly and interpreted in the specified timezone
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    // For RECURRING jobs
    private String cronExpression;

    // Time zone for the job
    private String timeZone;

    // For WEEKLY recurring jobs - days of the week (1-7 for Monday-Sunday)
    private List<Integer> daysOfWeek;

    // For MONTHLY recurring jobs - days of the month (1-31)
    private List<Integer> daysOfMonth;

    // For HOURLY recurring jobs - hours interval
    private Integer hourlyInterval;

    // Additional fields for recurring jobs
    private Integer recurringTimeHour;
    private Integer recurringTimeMinute;
}