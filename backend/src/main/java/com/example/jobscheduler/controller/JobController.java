package com.example.jobscheduler.controller;

import com.example.jobscheduler.dto.JobRequest;
import com.example.jobscheduler.dto.JobResponse;
import com.example.jobscheduler.model.Job; // Ensure this is the correct package for the Job class
import com.example.jobscheduler.exception.PastScheduleTimeException;
import com.example.jobscheduler.service.AsyncJobService;
import com.example.jobscheduler.service.JobSchedulerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for job operations.
 * Provides endpoints for creating, retrieving, updating, and deleting jobs.
 */
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobController {

    private final JobSchedulerService jobSchedulerService;
    private final AsyncJobService asyncJobService;

    /**
     * Creates a new job asynchronously.
     * Returns a response immediately and schedules the job in the background.
     *
     * @param jobRequest the job request
     * @return the created job response
     */
    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest jobRequest) {
        log.info("Received request to create job for clientId: {}", jobRequest.getClientId());
        log.info("Job request details: scheduleType={}, timeZone={}, startTime={}",
                jobRequest.getScheduleType(), jobRequest.getTimeZone(), jobRequest.getStartTime());

        try {
            JobResponse jobResponse = jobSchedulerService.createJob(jobRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(jobResponse);
        } catch (PastScheduleTimeException e) {
            log.warn("Attempted to schedule job in the past: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(JobResponse.builder()
                            .status(Job.JobStatus.COMPLETED_FAILURE)
                            .error("Cannot schedule jobs in the past. Please select a future time.")
                            .build());
        }
    }

    /**
     * Gets all jobs.
     *
     * @return a list of all jobs
     */
    @GetMapping
    public ResponseEntity<List<JobResponse>> getAllJobs() {
        log.info("Received request to get all jobs");
        List<JobResponse> jobs = jobSchedulerService.getAllJobs();
        return ResponseEntity.ok(jobs);
    }

    /**
     * Gets a job by ID.
     *
     * @param id the job ID
     * @return the job response
     */
    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable UUID id) {
        log.info("Received request to get job with ID: {}", id);
        try {
            JobResponse job = jobSchedulerService.getJobById(id);
            return ResponseEntity.ok(job);
        } catch (IllegalArgumentException e) {
            log.warn("Job not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deletes a job by ID asynchronously.
     *
     * @param id the job ID
     * @return no content if the job deletion was initiated successfully
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID id) {
        log.info("Received request to delete job with ID: {}", id);
        try {
            // Initiate asynchronous deletion
            asyncJobService.deleteJobAsync(id);
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            log.error("Error initiating job deletion: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Pauses a job by ID asynchronously.
     *
     * @param id the job ID
     * @return accepted response indicating the pause operation has been initiated
     */
    @PatchMapping("/{id}/pause")
    public ResponseEntity<Void> pauseJob(@PathVariable UUID id) {
        log.info("Received request to pause job with ID: {}", id);
        try {
            // Initiate asynchronous pause
            asyncJobService.pauseJobAsync(id);
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            log.error("Error initiating job pause: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Resumes a job by ID asynchronously.
     *
     * @param id the job ID
     * @return accepted response indicating the resume operation has been initiated
     */
    @PatchMapping("/{id}/resume")
    public ResponseEntity<Void> resumeJob(@PathVariable UUID id) {
        log.info("Received request to resume job with ID: {}", id);
        try {
            // Initiate asynchronous resume
            asyncJobService.resumeJobAsync(id);
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            log.error("Error initiating job resume: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}