package com.example.jobscheduler.controller;

import com.example.jobscheduler.dto.JobRequest;
import com.example.jobscheduler.dto.JobResponse;
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
 */
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobController {

    private final JobSchedulerService jobSchedulerService;

    /**
     * Creates a new job.
     *
     * @param jobRequest the job request
     * @return the created job response
     */
    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest jobRequest) {
        log.info("Received request to create job for clientId: {}", jobRequest.getClientId());
        JobResponse jobResponse = jobSchedulerService.createJob(jobRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(jobResponse);
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
     * Deletes a job by ID.
     *
     * @param id the job ID
     * @return no content if the job was deleted successfully
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID id) {
        log.info("Received request to delete job with ID: {}", id);
        try {
            jobSchedulerService.deleteJob(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Job not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Pauses a job by ID.
     *
     * @param id the job ID
     * @return the updated job response
     */
    @PatchMapping("/{id}/pause")
    public ResponseEntity<JobResponse> pauseJob(@PathVariable UUID id) {
        log.info("Received request to pause job with ID: {}", id);
        try {
            JobResponse job = jobSchedulerService.pauseJob(id);
            return ResponseEntity.ok(job);
        } catch (IllegalArgumentException e) {
            log.warn("Job not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            log.error("Error pausing job: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Resumes a job by ID.
     *
     * @param id the job ID
     * @return the updated job response
     */
    @PatchMapping("/{id}/resume")
    public ResponseEntity<JobResponse> resumeJob(@PathVariable UUID id) {
        log.info("Received request to resume job with ID: {}", id);
        try {
            JobResponse job = jobSchedulerService.resumeJob(id);
            return ResponseEntity.ok(job);
        } catch (IllegalArgumentException e) {
            log.warn("Job not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            log.error("Error resuming job: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}