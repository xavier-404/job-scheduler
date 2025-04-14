package com.example.jobscheduler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Entity representing a scheduled job in the system.
 * A job fetches user data for a specific clientId and publishes it to Kafka.
 */
@Entity // Marks this class as a JPA entity mapped to a database table.
@Table(name = "jobs") // Specifies the table name in the database.
@Data // Lombok annotation to generate getters, setters, toString, equals, and hashCode methods.
@Builder // Lombok annotation to implement the builder pattern for this class.
@NoArgsConstructor // Lombok annotation to generate a no-argument constructor.
@AllArgsConstructor // Lombok annotation to generate an all-argument constructor.
public class Job {

    @Id // Marks this field as the primary key of the entity.
    @GeneratedValue(strategy = GenerationType.AUTO) // Specifies that the ID will be generated automatically.
    private UUID id; // Unique identifier for the job.

    @Column(name = "client_id", nullable = false) // Maps this field to the "client_id" column in the database. It cannot be null.
    private String clientId; // Identifier for the client associated with the job.

    @Enumerated(EnumType.STRING) // Specifies that the enum value will be stored as a string in the database.
    @Column(name = "schedule_type", nullable = false) // Maps this field to the "schedule_type" column in the database. It cannot be null.
    private ScheduleType scheduleType; // Type of schedule for the job (e.g., IMMEDIATE, ONE_TIME, RECURRING).

    @Column(name = "cron_expression") // Maps this field to the "cron_expression" column in the database.
    private String cronExpression; // Cron expression defining the schedule for recurring jobs.

    @Column(name = "time_zone", nullable = false) // Maps this field to the "time_zone" column in the database. It cannot be null.
    private String timeZone = ZoneId.systemDefault().getId(); // Time zone for the job. Defaults to the system's time zone.

    @Column(name = "start_time") // Maps this field to the "start_time" column in the database.
    private LocalDateTime startTime; // Start time for the job.

    @Column(name = "next_fire_time") // Maps this field to the "next_fire_time" column in the database.
    private LocalDateTime nextFireTime; // The next scheduled execution time for the job.

    @Enumerated(EnumType.STRING) // Specifies that the enum value will be stored as a string in the database.
    @Column(nullable = false) // Maps this field to a database column. It cannot be null.
    private JobStatus status; // Current status of the job (e.g., SCHEDULED, RUNNING, COMPLETED_SUCCESS, COMPLETED_FAILURE).

    @CreationTimestamp // Automatically sets the timestamp when the entity is created.
    @Column(name = "created_at", updatable = false) // Maps this field to the "created_at" column. It cannot be updated after creation.
    private LocalDateTime createdAt; // Timestamp of when the job was created.

    @UpdateTimestamp // Automatically updates the timestamp when the entity is updated.
    @Column(name = "updated_at") // Maps this field to the "updated_at" column.
    private LocalDateTime updatedAt; // Timestamp of the last update to the job.

    /**
     * Types of job schedules.
     * IMMEDIATE: The job is executed immediately.
     * ONE_TIME: The job is executed once at a specific time.
     * RECURRING: The job is executed repeatedly based on a cron expression.
     */
    public enum ScheduleType {
        IMMEDIATE,
        ONE_TIME,
        RECURRING
    }

    /**
     * Possible job statuses.
     * SCHEDULED: The job is scheduled but not yet running.
     * RUNNING: The job is currently being executed.
     * COMPLETED_SUCCESS: The job completed successfully.
     * COMPLETED_FAILURE: The job completed but failed.
     */
    public enum JobStatus {
        SCHEDULED,
        RUNNING,
        COMPLETED_SUCCESS,
        COMPLETED_FAILURE
    }
}