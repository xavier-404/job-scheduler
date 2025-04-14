package com.example.jobscheduler.job;

import com.example.jobscheduler.model.Job;
import com.example.jobscheduler.model.User;
import com.example.jobscheduler.repository.JobRepository;
import com.example.jobscheduler.repository.UserRepository;
import com.example.jobscheduler.service.KafkaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Quartz job that fetches user data for a specific client ID and publishes it to Kafka.
 * This job is triggered by the Quartz scheduler based on the job schedule.
 */
@Component // Marks this class as a Spring-managed component.
@Slf4j // Lombok annotation to enable logging in this class.
public class UserDataJob extends QuartzJobBean {

    @Autowired
    private UserRepository userRepository; // Repository for fetching user data from the database.

    @Autowired
    private JobRepository jobRepository; // Repository for managing job entities in the database.

    @Autowired
    private KafkaService kafkaService; // Service for publishing user data to Kafka.

    /**
     * Executes the job.
     * 1. Fetches users for the specified client ID.
     * 2. Publishes each user to Kafka.
     * 3. Updates the job status and next fire time.
     *
     * @param context the job execution context
     * @throws JobExecutionException if an error occurs during job execution
     */
    @Override
    @Transactional // Ensures the method runs within a transactional context.
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        // Extract job details from the Quartz job execution context.
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        String jobId = jobDataMap.getString("jobId");
        String clientId = jobDataMap.getString("clientId");
        String timeZone = jobDataMap.getString("timeZone");

        log.info("Executing job {} for clientId {} in timezone {}", jobId, clientId, timeZone);

        // Log the next fire time if available.
        if (context.getNextFireTime() != null) {
            log.info("Next fire time from context: {}", context.getNextFireTime());
        } else {
            log.warn("No next fire time in context for job {}", jobId);
        }

        try {
            // Fetch the job entity from the database.
            Job job = jobRepository.findById(UUID.fromString(jobId))
                    .orElseThrow(() -> new JobExecutionException("Job not found: " + jobId));

            // Update the job status to RUNNING.
            log.info("Updating job status to RUNNING for job {}", jobId);
            job.setStatus(Job.JobStatus.RUNNING);
            jobRepository.save(job);

            // Fetch users associated with the client ID.
            log.info("Fetching users from database for clientId: {}", clientId);
            List<User> users = userRepository.findByClientId(clientId);
            log.info("Database query complete. Found {} users for clientId {}", users.size(), clientId);

            // Log user details if users are found.
            if (!users.isEmpty()) {
                log.info("User details for clientId {}:", clientId);
                users.forEach(user -> log.info("  User ID: {}, Name: {}, Email: {}",
                        user.getId(), user.getName(), user.getEmail()));
            }

            // If no users are found, mark the job as COMPLETED_SUCCESS and return.
            if (users.isEmpty()) {
                log.warn("No users found for clientId {}. Job will complete with success status.", clientId);
                job.setStatus(Job.JobStatus.COMPLETED_SUCCESS);

                // Update the next fire time for recurring jobs.
                updateNextFireTime(context, job);

                jobRepository.save(job);
                return;
            }

            // Publish each user to Kafka.
            log.info("Beginning to publish {} user records to Kafka topic for job {}", users.size(), jobId);
            List<CompletableFuture<Void>> futures = users.stream()
                    .map(user -> {
                        log.info("Publishing user {} to Kafka", user.getId());
                        return publishUserToKafka(user);
                    })
                    .collect(Collectors.toList());

            // Wait for all Kafka publish operations to complete.
            log.info("Waiting for all Kafka publish operations to complete for job {}", jobId);
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
            log.info("All Kafka publish operations completed for job {}", jobId);

            // Update the job status to COMPLETED_SUCCESS.
            log.info("Updating job status to COMPLETED_SUCCESS for job {}", jobId);
            job.setStatus(Job.JobStatus.COMPLETED_SUCCESS);

            // Update the next fire time for recurring jobs.
            updateNextFireTime(context, job);

            jobRepository.save(job);

            log.info("Job {} completed successfully, published {} user records to Kafka", jobId, users.size());
        } catch (Exception e) {
            log.error("Error executing job {}: {}", jobId, e.getMessage(), e);

            try {
                // Update the job status to COMPLETED_FAILURE.
                log.info("Updating job status to COMPLETED_FAILURE for job {}", jobId);
                Job job = jobRepository.findById(UUID.fromString(jobId))
                        .orElseThrow(() -> new JobExecutionException("Job not found: " + jobId));
                job.setStatus(Job.JobStatus.COMPLETED_FAILURE);

                // Update the next fire time for recurring jobs even if the job failed.
                updateNextFireTime(context, job);

                jobRepository.save(job);
            } catch (Exception ex) {
                log.error("Error updating job status: {}", ex.getMessage(), ex);
            }

            throw new JobExecutionException(e);
        }
    }

    /**
     * Publishes a user to Kafka.
     * Creates a CompletableFuture that completes when the user is published to Kafka.
     *
     * @param user the user to publish
     * @return a CompletableFuture that completes when the publish operation is done
     */
    private CompletableFuture<Void> publishUserToKafka(User user) {
        try {
            return kafkaService.publishUser(user).thenApply(result -> null);
        } catch (JsonProcessingException e) {
            log.error("Error serializing user {} to JSON: {}", user.getId(), e.getMessage());
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Updates the next fire time for a job if it's a recurring job.
     * Retrieves the next fire time from the job execution context and updates the job entity.
     *
     * @param context the job execution context
     * @param job     the job entity to update
     */
    private void updateNextFireTime(JobExecutionContext context, Job job) {
        try {
            if (context.getNextFireTime() != null) {
                ZoneId zoneId = ZoneId.of(job.getTimeZone());
                LocalDateTime nextFireTime = LocalDateTime.ofInstant(
                        context.getNextFireTime().toInstant(), zoneId);
                job.setNextFireTime(nextFireTime);
                log.info("Updated next fire time for job {}: {}", job.getId(), nextFireTime);
            } else if (job.getScheduleType() == Job.ScheduleType.RECURRING) {
                log.warn("No next fire time for recurring job {}", job.getId());
            }
        } catch (Exception e) {
            log.error("Error updating next fire time: {}", e.getMessage(), e);
        }
    }
}