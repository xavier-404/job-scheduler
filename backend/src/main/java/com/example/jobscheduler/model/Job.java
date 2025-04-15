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
@Entity
@Table(name = "jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false)
    private ScheduleType scheduleType;

    @Column(name = "cron_expression")
    private String cronExpression;

    @Column(name = "time_zone", nullable = false)
    private String timeZone = ZoneId.systemDefault().getId();

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "next_fire_time")
    private LocalDateTime nextFireTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
     * SCHEDULING: The job is in the process of being scheduled.
     * SCHEDULED: The job is scheduled but not yet running.
     * RUNNING: The job is currently being executed.
     * COMPLETED_SUCCESS: The job completed successfully.
     * COMPLETED_FAILURE: The job completed but failed.
     */
    public enum JobStatus {
        SCHEDULING,
        SCHEDULED,
        RUNNING,
        COMPLETED_SUCCESS,
        COMPLETED_FAILURE
    }
}