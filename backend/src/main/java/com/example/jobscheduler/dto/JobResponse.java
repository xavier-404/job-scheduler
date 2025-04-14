package com.example.jobscheduler.dto;

import com.example.jobscheduler.model.Job;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO (Data Transfer Object) for job responses to the client.
 * Contains all job information to be displayed in the frontend.
 */
@Data // Lombok annotation to generate getters, setters, toString, equals, and hashCode methods.
@Builder // Lombok annotation to implement the builder pattern for this class.
@NoArgsConstructor // Lombok annotation to generate a no-argument constructor.
@AllArgsConstructor // Lombok annotation to generate an all-argument constructor.
public class JobResponse {

    private UUID id; // Unique identifier for the job.
    private String clientId; // Identifier for the client associated with the job.
    private Job.ScheduleType scheduleType; // Type of schedule for the job (e.g., IMMEDIATE, ONE_TIME, RECURRING).
    private String cronExpression; // Cron expression defining the schedule for recurring jobs.
    private String timeZone; // Time zone in which the job is executed.
    private LocalDateTime startTime; // Start time for the job (applicable for ONE_TIME jobs).
    private LocalDateTime nextFireTime; // The next scheduled execution time for the job.
    private Job.JobStatus status; // Current status of the job (e.g., SCHEDULED, RUNNING, COMPLETED_SUCCESS, COMPLETED_FAILURE).
    private LocalDateTime createdAt; // Timestamp of when the job was created.
    private LocalDateTime updatedAt; // Timestamp of the last update to the job.

    /**
     * Converts a Job entity to a JobResponse DTO.
     * This method is used to map the Job entity from the database to a JobResponse object
     * that can be sent to the client.
     *
     * @param job the job entity to convert
     * @return a new JobResponse DTO containing the job's details
     */
    public static JobResponse fromEntity(Job job) {
        return JobResponse.builder()
                .id(job.getId()) // Maps the job's unique ID.
                .clientId(job.getClientId()) // Maps the client ID associated with the job.
                .scheduleType(job.getScheduleType()) // Maps the schedule type of the job.
                .cronExpression(job.getCronExpression()) // Maps the cron expression for recurring jobs.
                .timeZone(job.getTimeZone()) // Maps the time zone of the job.
                .startTime(job.getStartTime()) // Maps the start time for ONE_TIME jobs.
                .nextFireTime(job.getNextFireTime()) // Maps the next scheduled execution time.
                .status(job.getStatus()) // Maps the current status of the job.
                .createdAt(job.getCreatedAt()) // Maps the creation timestamp of the job.
                .updatedAt(job.getUpdatedAt()) // Maps the last updated timestamp of the job.
                .build(); // Builds and returns the JobResponse object.
    }
}