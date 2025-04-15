package com.example.jobscheduler.service;

import com.example.jobscheduler.dto.JobRequest;
import com.example.jobscheduler.dto.JobResponse;
import com.example.jobscheduler.exception.AsyncJobSchedulingException;
import com.example.jobscheduler.model.Job;
import com.example.jobscheduler.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for handling asynchronous job scheduling operations.
 * This service delegates to JobSchedulerService but executes operations asynchronously.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AsyncJobService {

    private final JobSchedulerService jobSchedulerService;
    private final JobRepository jobRepository;

    /**
     * Asynchronously creates and schedules a job.
     * Allows the REST API to return immediately while scheduling happens in the background.
     *
     * @param jobRequest the job request containing scheduling details
     * @return a CompletableFuture that completes with the created job response
     */
    @Async("jobSchedulerTaskExecutor")
    public CompletableFuture<JobResponse> scheduleJobAsync(JobRequest jobRequest) {
        log.info("Scheduling job asynchronously for clientId: {}", jobRequest.getClientId());
        
        try {
            // Just delegate to the existing service instead of creating a placeholder
            // This avoids the concurrency issue with multiple transactions
            JobResponse finalResponse = jobSchedulerService.createJob(jobRequest);
            
            log.info("Job scheduled asynchronously for clientId: {}, jobId: {}", 
                    jobRequest.getClientId(), finalResponse.getId());
            
            return CompletableFuture.completedFuture(finalResponse);
        } catch (Exception e) {
            log.error("Failed to schedule job asynchronously: {}", e.getMessage(), e);
            throw new AsyncJobSchedulingException("Failed to schedule job: " + e.getMessage(), e);
        }
    }
    
    /**
     * Asynchronously deletes a job.
     *
     * @param jobId the ID of the job to delete
     * @return a CompletableFuture that completes when the job is deleted
     */
    @Async("jobSchedulerTaskExecutor")
    public CompletableFuture<Void> deleteJobAsync(UUID jobId) {
        log.info("Deleting job asynchronously, jobId: {}", jobId);
        
        try {
            jobSchedulerService.deleteJob(jobId);
            log.info("Job deleted asynchronously, jobId: {}", jobId);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to delete job asynchronously: {}", e.getMessage(), e);
            throw new AsyncJobSchedulingException("Failed to delete job: " + e.getMessage(), e);
        }
    }
    
    /**
     * Asynchronously pauses a job.
     *
     * @param jobId the ID of the job to pause
     * @return a CompletableFuture that completes with the updated job response
     */
    @Async("jobSchedulerTaskExecutor")
    public CompletableFuture<JobResponse> pauseJobAsync(UUID jobId) {
        log.info("Pausing job asynchronously, jobId: {}", jobId);
        
        try {
            JobResponse response = jobSchedulerService.pauseJob(jobId);
            log.info("Job paused asynchronously, jobId: {}", jobId);
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            log.error("Failed to pause job asynchronously: {}", e.getMessage(), e);
            throw new AsyncJobSchedulingException("Failed to pause job: " + e.getMessage(), e);
        }
    }
    
    /**
     * Asynchronously resumes a job.
     *
     * @param jobId the ID of the job to resume
     * @return a CompletableFuture that completes with the updated job response
     */
    @Async("jobSchedulerTaskExecutor")
    public CompletableFuture<JobResponse> resumeJobAsync(UUID jobId) {
        log.info("Resuming job asynchronously, jobId: {}", jobId);
        
        try {
            JobResponse response = jobSchedulerService.resumeJob(jobId);
            log.info("Job resumed asynchronously, jobId: {}", jobId);
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            log.error("Failed to resume job asynchronously: {}", e.getMessage(), e);
            throw new AsyncJobSchedulingException("Failed to resume job: " + e.getMessage(), e);
        }
    }
}