package com.example.jobscheduler.service;

import com.example.jobscheduler.dto.JobRequest;
import com.example.jobscheduler.dto.JobResponse;
import com.example.jobscheduler.exception.InvalidTimeZoneException;
import com.example.jobscheduler.exception.PastScheduleTimeException;
import com.example.jobscheduler.job.UserDataJob;
import com.example.jobscheduler.model.Job;
import com.example.jobscheduler.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for scheduling and managing jobs.
 * Handles the creation, retrieval, updating, and deletion of jobs.
 * Also manages the scheduling of jobs using Quartz Scheduler.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JobSchedulerService {

    // Define "Asia/Kolkata" as the default timezone for the entire application
    public static final String DEFAULT_TIMEZONE = "Asia/Kolkata";

    private final JobRepository jobRepository;
    private final Scheduler scheduler;

    // Set of valid timezones, initialized in postConstruct
    private Set<String> validTimeZones;

    /**
     * Initialize valid timezones after dependency injection is complete.
     */
    @Autowired
    public void initialize() {
        this.validTimeZones = ZoneId.getAvailableZoneIds();
        log.info("Initialized JobSchedulerService with {} available timezones. Default timezone: {}",
                validTimeZones.size(), DEFAULT_TIMEZONE);
    }

    /**
     * Creates a new job based on the job request.
     * Validates the job request, persists it to the database, and schedules it with
     * Quartz.
     *
     * @param jobRequest the job request with scheduling details
     * @return the created job response
     * @throws InvalidTimeZoneException  if the provided timezone is invalid
     * @throws PastScheduleTimeException if the scheduled time is in the past
     */
    @Transactional
    public JobResponse createJob(JobRequest jobRequest) {
        String clientId = jobRequest.getClientId();
        log.info("Creating job for clientId: {}", clientId);

        // Validate the timezone
        String timeZone = validateTimeZone(jobRequest.getTimeZone());

        // Create a new Job entity from the request
        Job job = createJobEntity(jobRequest, timeZone);

        // Validate the job timing is not in the past
        validateJobTiming(job);

        // Validate the cron expression for RECURRING jobs
        if (job.getScheduleType() == Job.ScheduleType.RECURRING && job.getCronExpression() != null) {
            try {
                CronExpression.validateExpression(job.getCronExpression());
                log.info("Validated cron expression: {}", job.getCronExpression());
            } catch (Exception e) {
                log.error("Invalid cron expression: {}", job.getCronExpression(), e);
                throw new IllegalArgumentException("Invalid cron expression: " + job.getCronExpression());
            }
        }

        // Save the job to the database to generate an ID
        final Job savedJob = jobRepository.save(job);
        final UUID jobId = savedJob.getId();

        // Schedule the job after the transaction commits to avoid race conditions
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    log.info("Transaction committed. Scheduling job {} for clientId {}", jobId, clientId);

                    // Fetch the latest version of the job from the database
                    Job jobToSchedule = jobRepository.findById(jobId)
                            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

                    // Schedule the job with Quartz
                    Trigger trigger = scheduleJobWithQuartz(jobToSchedule);

                    // Update the next fire time after scheduling
                    if (trigger.getNextFireTime() != null) {
                        // Always convert to the job's timezone for display
                        ZonedDateTime nextFireZdt = trigger.getNextFireTime().toInstant()
                                .atZone(ZoneId.of(jobToSchedule.getTimeZone()));
                        LocalDateTime nextFireTime = nextFireZdt.toLocalDateTime();

                        log.info("Job {} scheduled. Next fire time: {} ({})",
                                jobId, nextFireTime, jobToSchedule.getTimeZone());

                        // Update the job's next fire time in a new transaction
                        updateJobNextFireTime(jobId, nextFireTime);
                    } else {
                        log.warn("No next fire time calculated for job {}", jobId);
                    }
                } catch (Exception e) {
                    log.error("Error scheduling job {}: {}", jobId, e.getMessage(), e);
                    // Mark the job as failed in a new transaction
                    updateJobStatus(jobId, Job.JobStatus.COMPLETED_FAILURE);
                }
            }
        });

        return JobResponse.fromEntity(savedJob);
    }

    /**
     * Validates that the provided timezone is valid.
     * 
     * @param timeZone the timezone to validate
     * @return the validated timezone
     * @throws InvalidTimeZoneException if the timezone is invalid
     */
    private String validateTimeZone(String timeZone) {
        if (timeZone == null || timeZone.trim().isEmpty()) {
            log.info("No timezone provided, defaulting to {}", DEFAULT_TIMEZONE);
            return DEFAULT_TIMEZONE;
        }

        if (validTimeZones == null) {
            // Initialize timezones if they haven't been initialized yet
            validTimeZones = ZoneId.getAvailableZoneIds();
        }

        if (!validTimeZones.contains(timeZone)) {
            log.error("Invalid timezone provided: {}", timeZone);
            throw new InvalidTimeZoneException("Invalid timezone: " + timeZone);
        }

        return timeZone;
    }

    /**
     * Validates that the job's scheduled time is not in the past.
     * 
     * @param job the job to validate
     * @throws PastScheduleTimeException if the scheduled time is in the past
     */
    /**
     * Validates that the job's scheduled time is not in the past.
     */
    private void validateJobTiming(Job job) {
        if (job.getScheduleType() == Job.ScheduleType.ONE_TIME && job.getStartTime() != null) {
            // Get the client's timezone
            ZoneId clientZoneId = ZoneId.of(job.getTimeZone());

            // Get current time in the client's timezone
            ZonedDateTime nowInClientTz = ZonedDateTime.now(clientZoneId);

            // Job start time in client's timezone
            ZonedDateTime jobStartTimeInClientTz = job.getStartTime().atZone(clientZoneId);

            log.info("Validation: Current time in {}: {}", job.getTimeZone(), nowInClientTz);
            log.info("Validation: Job start time in {}: {}", job.getTimeZone(), jobStartTimeInClientTz);

            if (jobStartTimeInClientTz.isBefore(nowInClientTz)) {
                // Add small buffer for processing time (30 seconds)
                if (jobStartTimeInClientTz.plusSeconds(30).isBefore(nowInClientTz)) {
                    log.warn("Validation failed: Job scheduled for past time: {} in timezone {}",
                            jobStartTimeInClientTz, job.getTimeZone());

                    throw new PastScheduleTimeException(
                            "Cannot schedule job in the past. Current time in " +
                                    job.getTimeZone() + " is " + nowInClientTz.toLocalDateTime() +
                                    " but job was scheduled for " + jobStartTimeInClientTz.toLocalDateTime());
                } else {
                    log.info("Job scheduled very close to current time (within buffer)");
                }
            }
        }
    }

    /**
     * Retrieves all jobs from the database.
     *
     * @return a list of all jobs as JobResponse objects
     */
    public List<JobResponse> getAllJobs() {
        List<Job> jobs = jobRepository.findAll();
        log.debug("Retrieved {} jobs from database", jobs.size());
        return jobs.stream()
                .map(JobResponse::fromEntity)
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
        log.debug("Retrieved job with ID: {}, clientId: {}", id, job.getClientId());
        return JobResponse.fromEntity(job);
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

        log.info("Deleting job with ID: {}, clientId: {}", id, job.getClientId());

        try {
            // Unschedule the job in Quartz
            JobKey jobKey = new JobKey(job.getId().toString());
            scheduler.deleteJob(jobKey);
            log.debug("Job deleted from Quartz scheduler: {}", id);
        } catch (SchedulerException e) {
            log.error("Error deleting job from scheduler: {}", e.getMessage(), e);
            // Continue with database deletion even if Quartz deletion fails
        }

        // Delete the job from the database
        jobRepository.delete(job);
        log.info("Job deleted successfully: {}", id);
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

        log.info("Pausing job with ID: {}, clientId: {}", id, job.getClientId());

        try {
            // Pause the job in Quartz
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

        log.info("Resuming job with ID: {}, clientId: {}", id, job.getClientId());

        try {
            // Resume the job in Quartz
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
            log.debug("Updated next fire time for job {}: {}", jobId, nextFireTime);
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
            log.debug("Updated status for job {}: {}", jobId, status);
        } catch (Exception e) {
            log.error("Error updating status for job {}: {}", jobId, e.getMessage(), e);
        }
    }

    /**
     * Creates a new job entity based on the job request.
     *
     * @param jobRequest the job request
     * @param timeZone   the validated timezone
     * @return the new job entity
     */
    private Job createJobEntity(JobRequest jobRequest, String timeZone) {
        Job job = new Job();
        job.setClientId(jobRequest.getClientId());
        job.setScheduleType(jobRequest.getScheduleType());
        job.setStatus(Job.JobStatus.SCHEDULED);
        job.setTimeZone(timeZone); // Store the user's selected timezone

        ZoneId zoneId = ZoneId.of(timeZone);
        log.info("Creating job entity with timezone: {}", timeZone);

        switch (jobRequest.getScheduleType()) {
            case IMMEDIATE:
                job.setStartTime(LocalDateTime.now(zoneId));
                log.info("IMMEDIATE job: Setting startTime to now in timezone {}: {}",
                        timeZone, job.getStartTime());
                break;

            case ONE_TIME:
                if (jobRequest.getStartTime() != null) {
                    // The startTime is interpreted in the client's selected timezone
                    LocalDateTime startTime = jobRequest.getStartTime();
                    log.info("ONE_TIME job: Setting startTime: {} in timezone: {}",
                            startTime, timeZone);

                    job.setStartTime(startTime);
                    job.setNextFireTime(startTime);

                    // Log the equivalent time in server timezone for debugging
                    ZonedDateTime clientTime = startTime.atZone(zoneId);
                    ZonedDateTime serverTime = clientTime.withZoneSameInstant(ZoneId.systemDefault());
                    log.info("Job time in server timezone ({}): {}",
                            ZoneId.systemDefault(), serverTime);
                }
                break;

            case RECURRING:
                job.setStartTime(LocalDateTime.now(zoneId));
                job.setCronExpression(determineCronExpression(jobRequest));
                log.info("RECURRING job: startTime={}, cronExpression={}",
                        job.getStartTime(), job.getCronExpression());
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
        // If cron expression is provided directly, use it
        if (jobRequest.getCronExpression() != null && !jobRequest.getCronExpression().isEmpty()) {
            return jobRequest.getCronExpression();
        }

        // Get time components or use defaults
        int hour = jobRequest.getRecurringTimeHour() != null ? jobRequest.getRecurringTimeHour() : 0;
        int minute = jobRequest.getRecurringTimeMinute() != null ? jobRequest.getRecurringTimeMinute() : 0;

        // HOURLY: Run every X hours
        if (jobRequest.getHourlyInterval() != null) {
            return String.format("0 %d 0/%d * * ?", minute, jobRequest.getHourlyInterval());
        }

        // WEEKLY: Run on specific days of the week
        if (jobRequest.getDaysOfWeek() != null && !jobRequest.getDaysOfWeek().isEmpty()) {
            String daysOfWeek = jobRequest.getDaysOfWeek().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(","));
            return String.format("0 %d %d ? * %s", minute, hour, daysOfWeek);
        }

        // MONTHLY: Run on specific days of the month
        if (jobRequest.getDaysOfMonth() != null && !jobRequest.getDaysOfMonth().isEmpty()) {
            String daysOfMonth = jobRequest.getDaysOfMonth().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(","));
            return String.format("0 %d %d %s * ?", minute, hour, daysOfMonth);
        }

        // Default: Run daily at the specified hour and minute
        return String.format("0 %d %d * * ?", minute, hour);
    }

    /**
     * Schedules a job with Quartz.
     *
     * @param job the job to schedule
     * @return the trigger used to schedule the job
     * @throws SchedulerException if an error occurs during scheduling
     */
    private Trigger scheduleJobWithQuartz(Job job) throws SchedulerException {
        // Create job detail with all necessary job data
        JobDetail jobDetail = JobBuilder.newJob(UserDataJob.class)
                .withIdentity(job.getId().toString())
                .usingJobData("jobId", job.getId().toString())
                .usingJobData("clientId", job.getClientId())
                .usingJobData("timeZone", job.getTimeZone())
                .build();

        // Create trigger based on schedule type
        Trigger trigger;
        ZoneId clientZoneId = ZoneId.of(job.getTimeZone());
        log.info("Scheduling job with Quartz using timezone: {}", job.getTimeZone());

        switch (job.getScheduleType()) {
            case IMMEDIATE:
                trigger = TriggerBuilder.newTrigger()
                        .withIdentity(job.getId().toString() + "_trigger")
                        .startNow()
                        .build();
                log.info("IMMEDIATE trigger created");
                break;

            case ONE_TIME:
                // Convert the client timezone date to a UTC instant for Quartz
                ZonedDateTime zonedDateTime = job.getStartTime().atZone(clientZoneId);
                Date startDate = Date.from(zonedDateTime.toInstant());

                // Log various timezone representations for debugging
                ZonedDateTime utcTime = zonedDateTime.withZoneSameInstant(ZoneId.of("UTC"));
                ZonedDateTime serverTime = zonedDateTime.withZoneSameInstant(ZoneId.systemDefault());

                log.info("Scheduling job {} for execution:", job.getId());
                log.info("- In client timezone ({}): {}", job.getTimeZone(), zonedDateTime);
                log.info("- In UTC: {}", utcTime);
                log.info("- In server timezone ({}): {}", ZoneId.systemDefault(), serverTime);
                log.info("- As Date for Quartz: {}", startDate);

                trigger = TriggerBuilder.newTrigger()
                        .withIdentity(job.getId().toString() + "_trigger")
                        .startAt(startDate)
                        .build();

                log.info("Trigger created with startTime: {}", trigger.getStartTime());
                break;

            case RECURRING:
                CronScheduleBuilder cronSchedule = CronScheduleBuilder
                        .cronSchedule(job.getCronExpression())
                        .inTimeZone(java.util.TimeZone.getTimeZone(clientZoneId));

                trigger = TriggerBuilder.newTrigger()
                        .withIdentity(job.getId().toString() + "_trigger")
                        .withSchedule(cronSchedule)
                        .build();

                log.info("RECURRING trigger created with cron: {}, next fire time: {}",
                        job.getCronExpression(), trigger.getNextFireTime());
                break;

            default:
                throw new IllegalArgumentException("Unsupported schedule type: " + job.getScheduleType());
        }

        scheduler.scheduleJob(jobDetail, trigger);
        log.info("Job scheduled with Quartz: ID={}, timeZone={}, type={}, next fire time={}",
                job.getId(), job.getTimeZone(), job.getScheduleType(), trigger.getNextFireTime());

        return trigger;
    }
}