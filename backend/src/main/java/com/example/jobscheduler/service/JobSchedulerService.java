package com.example.jobscheduler.service;

import com.example.jobscheduler.dto.JobRequest;
import com.example.jobscheduler.dto.JobResponse;
import com.example.jobscheduler.job.UserDataJob;
import com.example.jobscheduler.model.Job;
import com.example.jobscheduler.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for scheduling and managing jobs.
 * Handles the creation, retrieval, updating, and deletion of jobs.
 * Also manages the scheduling of jobs using Quartz Scheduler.
 */
@Service // Marks this class as a Spring service component.
@Slf4j // Lombok annotation to enable logging in this class.
@RequiredArgsConstructor // Lombok annotation to generate a constructor for all final fields.
public class JobSchedulerService {

    private final JobRepository jobRepository; // Repository for interacting with the Job database.
    private final Scheduler scheduler; // Quartz Scheduler for managing job scheduling.

    /**
     * Creates a new job based on the job request.
     * Validates the job request, persists it to the database, and schedules it with Quartz.
     *
     * @param jobRequest the job request with scheduling details
     * @return the created job response
     */
    @Transactional
    public JobResponse createJob(JobRequest jobRequest) {
        log.info("Creating job for clientId: {}", jobRequest.getClientId());

        // Create a new Job entity from the request.
        Job job = createJobEntity(jobRequest);

        // Validate the cron expression for RECURRING jobs.
        if (job.getScheduleType() == Job.ScheduleType.RECURRING && job.getCronExpression() != null) {
            try {
                CronExpression.validateExpression(job.getCronExpression());
                log.info("Validated cron expression: {}", job.getCronExpression());
            } catch (Exception e) {
                log.error("Invalid cron expression: {}", job.getCronExpression(), e);
                throw new IllegalArgumentException("Invalid cron expression: " + job.getCronExpression());
            }
        }

        // Save the job to the database to generate an ID.
        final Job savedJob = jobRepository.save(job);
        final UUID jobId = savedJob.getId();
        final String clientId = savedJob.getClientId();

        // Schedule the job after the transaction commits to avoid race conditions.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    log.info("Transaction committed. Scheduling job {} for clientId {}", jobId, clientId);

                    // Fetch the latest version of the job from the database.
                    Job jobToSchedule = jobRepository.findById(jobId)
                            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

                    // Schedule the job with Quartz.
                    Trigger trigger = scheduleJobWithQuartz(jobToSchedule);

                    // Update the next fire time after scheduling.
                    if (trigger.getNextFireTime() != null) {
                        LocalDateTime nextFireTime = LocalDateTime.ofInstant(
                                trigger.getNextFireTime().toInstant(),
                                ZoneId.of(jobToSchedule.getTimeZone()));

                        log.info("Job {} scheduled. Next fire time: {} ({})",
                                jobId, nextFireTime, jobToSchedule.getTimeZone());

                        // Update the job's next fire time in a new transaction.
                        updateJobNextFireTime(jobId, nextFireTime);
                    } else {
                        log.warn("No next fire time calculated for job {}", jobId);
                    }
                } catch (Exception e) {
                    log.error("Error scheduling job {}: {}", jobId, e.getMessage(), e);
                    // Mark the job as failed in a new transaction.
                    updateJobStatus(jobId, Job.JobStatus.COMPLETED_FAILURE);
                }
            }
        });

        return JobResponse.fromEntity(savedJob); // Return the created job as a response.
    }

    /**
     * Retrieves all jobs from the database.
     *
     * @return a list of all jobs as JobResponse objects
     */
    public List<JobResponse> getAllJobs() {
        List<Job> jobs = jobRepository.findAll(); // Fetch all jobs from the database.
        return jobs.stream()
                .map(JobResponse::fromEntity) // Convert each Job entity to a JobResponse DTO.
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a job by its ID.
     *
     * @param id the job ID
     * @return the job response
     * @throws IllegalArgumentException if the job is not found
     */
    public JobResponse getJobById(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
        return JobResponse.fromEntity(job); // Convert the Job entity to a JobResponse DTO.
    }

    /**
     * Deletes a job by its ID.
     * Removes the job from both the Quartz scheduler and the database.
     *
     * @param id the job ID
     * @throws IllegalArgumentException if the job is not found
     */
    @Transactional
    public void deleteJob(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));

        try {
            // Unschedule the job in Quartz.
            JobKey jobKey = new JobKey(job.getId().toString());
            scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            log.error("Error deleting job from scheduler: {}", e.getMessage(), e);
        }

        // Delete the job from the database.
        jobRepository.delete(job);
    }

    /**
     * Pauses a job by its ID in the Quartz scheduler.
     *
     * @param id the job ID
     * @return the updated job response
     * @throws IllegalArgumentException if the job is not found
     */
    @Transactional
    public JobResponse pauseJob(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));

        try {
            // Pause the job in Quartz.
            JobKey jobKey = new JobKey(job.getId().toString());
            scheduler.pauseJob(jobKey);
            log.info("Job paused: {}", id);
        } catch (SchedulerException e) {
            log.error("Error pausing job: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to pause job", e);
        }

        return JobResponse.fromEntity(job);
    }

    /**
     * Resumes a paused job by its ID in the Quartz scheduler.
     *
     * @param id the job ID
     * @return the updated job response
     * @throws IllegalArgumentException if the job is not found
     */
    @Transactional
    public JobResponse resumeJob(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));

        try {
            // Resume the job in Quartz.
            JobKey jobKey = new JobKey(job.getId().toString());
            scheduler.resumeJob(jobKey);
            log.info("Job resumed: {}", id);
        } catch (SchedulerException e) {
            log.error("Error resuming job: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to resume job", e);
        }

        return JobResponse.fromEntity(job);
    }

    /**
     * Updates the next fire time for a job in a new transaction.
     *
     * @param jobId        the job ID
     * @param nextFireTime the next fire time
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateJobNextFireTime(UUID jobId, LocalDateTime nextFireTime) {
        try {
            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
            job.setNextFireTime(nextFireTime);
            jobRepository.save(job);
            log.info("Updated next fire time for job {}: {}", jobId, nextFireTime);
        } catch (Exception e) {
            log.error("Error updating next fire time for job {}: {}", jobId, e.getMessage(), e);
        }
    }

    /**
     * Updates the status of a job in a new transaction.
     *
     * @param jobId  the job ID
     * @param status the new job status
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateJobStatus(UUID jobId, Job.JobStatus status) {
        try {
            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
            job.setStatus(status);
            jobRepository.save(job);
            log.info("Updated status for job {}: {}", jobId, status);
        } catch (Exception e) {
            log.error("Error updating status for job {}: {}", jobId, e.getMessage(), e);
        }
    }

    /**
     * Creates a new job entity based on the job request.
     *
     * @param jobRequest the job request
     * @return the new job entity
     */
    private Job createJobEntity(JobRequest jobRequest) {
        Job job = new Job();
        job.setClientId(jobRequest.getClientId());
        job.setScheduleType(jobRequest.getScheduleType());
        job.setStatus(Job.JobStatus.SCHEDULED);

        // Use the timezone provided by the user, with UTC as fallback.
        job.setTimeZone(jobRequest.getTimeZone() != null && !jobRequest.getTimeZone().isEmpty()
                ? jobRequest.getTimeZone()
                : "UTC");

        // Set start time and cron expression based on schedule type.
        switch (jobRequest.getScheduleType()) {
            case IMMEDIATE:
                job.setStartTime(LocalDateTime.now());
                break;
            case ONE_TIME:
                job.setStartTime(jobRequest.getStartTime());
                job.setNextFireTime(jobRequest.getStartTime());
                break;
            case RECURRING:
                job.setStartTime(LocalDateTime.now());
                job.setCronExpression(determineCronExpression(jobRequest));
                break;
        }

        return job;
    }

    /**
     * Determines the cron expression based on the job request.
     *
     * @param jobRequest the job request
     * @return the cron expression
     */
    private String determineCronExpression(JobRequest jobRequest) {
        // If cron expression is provided directly, use it.
        if (jobRequest.getCronExpression() != null && !jobRequest.getCronExpression().isEmpty()) {
            return jobRequest.getCronExpression();
        }

        // Get time components or use defaults.
        int hour = jobRequest.getRecurringTimeHour() != null ? jobRequest.getRecurringTimeHour() : 0;
        int minute = jobRequest.getRecurringTimeMinute() != null ? jobRequest.getRecurringTimeMinute() : 0;

        // HOURLY: Run every X hours.
        if (jobRequest.getHourlyInterval() != null) {
            return String.format("0 %d 0/%d * * ?", minute, jobRequest.getHourlyInterval());
        }

        // WEEKLY: Run on specific days of the week.
        if (jobRequest.getDaysOfWeek() != null && !jobRequest.getDaysOfWeek().isEmpty()) {
            String daysOfWeek = jobRequest.getDaysOfWeek().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(","));
            return String.format("0 %d %d ? * %s", minute, hour, daysOfWeek);
        }

        // MONTHLY: Run on specific days of the month.
        if (jobRequest.getDaysOfMonth() != null && !jobRequest.getDaysOfMonth().isEmpty()) {
            String daysOfMonth = jobRequest.getDaysOfMonth().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(","));
            return String.format("0 %d %d %s * ?", minute, hour, daysOfMonth);
        }

        // Default: Run every hour at the specified minute.
        return String.format("0 %d * * * ?", minute);
    }

    /**
     * Schedules a job with Quartz.
     *
     * @param job the job to schedule
     * @return the trigger used to schedule the job
     * @throws SchedulerException if an error occurs during scheduling
     */
    private Trigger scheduleJobWithQuartz(Job job) throws SchedulerException {
        // Create job detail with all necessary job data.
        JobDetail jobDetail = JobBuilder.newJob(UserDataJob.class)
                .withIdentity(job.getId().toString())
                .usingJobData("jobId", job.getId().toString())
                .usingJobData("clientId", job.getClientId())
                .usingJobData("timeZone", job.getTimeZone())
                .build();

        // Create trigger based on schedule type.
        Trigger trigger;
        ZoneId zoneId = ZoneId.of(job.getTimeZone());

        switch (job.getScheduleType()) {
            case IMMEDIATE:
                trigger = TriggerBuilder.newTrigger()
                        .withIdentity(job.getId().toString() + "_trigger")
                        .startNow()
                        .build();
                break;

            case ONE_TIME:
                ZonedDateTime zonedDateTime = job.getStartTime().atZone(zoneId);
                Date startDate = Date.from(zonedDateTime.toInstant());

                trigger = TriggerBuilder.newTrigger()
                        .withIdentity(job.getId().toString() + "_trigger")
                        .startAt(startDate)
                        .build();
                break;

            case RECURRING:
                CronScheduleBuilder cronSchedule = CronScheduleBuilder
                        .cronSchedule(job.getCronExpression())
                        .inTimeZone(java.util.TimeZone.getTimeZone(zoneId));

                trigger = TriggerBuilder.newTrigger()
                        .withIdentity(job.getId().toString() + "_trigger")
                        .withSchedule(cronSchedule)
                        .build();
                break;

            default:
                throw new IllegalArgumentException("Unsupported schedule type: " + job.getScheduleType());
        }

        // Schedule the job with Quartz.
        scheduler.scheduleJob(jobDetail, trigger);

        return trigger;
    }
}