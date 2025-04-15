package com.example.jobscheduler.service;

import com.example.jobscheduler.dto.JobRequest;
import com.example.jobscheduler.dto.JobResponse;
import com.example.jobscheduler.model.Job;
import com.example.jobscheduler.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncJobServiceTest {

    @Mock
    private JobSchedulerService jobSchedulerService;

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private AsyncJobService asyncJobService;

    private JobRequest jobRequest;
    private JobResponse jobResponse;
    private Job placeholderJob;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        // Initialize common test objects
        jobId = UUID.randomUUID();
        
        jobRequest = JobRequest.builder()
                .clientId("TEST_CLIENT")
                .scheduleType(Job.ScheduleType.ONE_TIME)
                .startTime(LocalDateTime.now().plusHours(1))
                .timeZone("UTC")
                .build();
        
        jobResponse = JobResponse.builder()
                .id(jobId)
                .clientId("TEST_CLIENT")
                .scheduleType(Job.ScheduleType.ONE_TIME)
                .status(Job.JobStatus.SCHEDULED)
                .timeZone("UTC")
                .build();
        
        placeholderJob = Job.builder()
                .id(jobId)
                .clientId("TEST_CLIENT")
                .scheduleType(Job.ScheduleType.ONE_TIME)
                .status(Job.JobStatus.SCHEDULING)
                .timeZone("UTC")
                .build();
        
        // Set up common mocks
        when(jobRepository.save(any(Job.class))).thenReturn(placeholderJob);
        when(jobSchedulerService.createJob(any(JobRequest.class))).thenReturn(jobResponse);
    }

    @Test
    void scheduleJobAsync_ShouldCreatePlaceholderAndScheduleJob() throws ExecutionException, InterruptedException {
        // Act
        CompletableFuture<JobResponse> future = asyncJobService.scheduleJobAsync(jobRequest);
        JobResponse result = future.get(); // Wait for the async operation to complete
        
        // Assert
        assertNotNull(result);
        assertEquals(jobId, result.getId());
        assertEquals("TEST_CLIENT", result.getClientId());
        assertEquals(Job.JobStatus.SCHEDULED, result.getStatus());
        
        // Verify interactions
        verify(jobRepository).save(any(Job.class));
        verify(jobSchedulerService).createJob(jobRequest);
        
        // Capture the placeholder job to verify its properties
        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(jobCaptor.capture());
        Job capturedJob = jobCaptor.getValue();
        
        assertEquals("TEST_CLIENT", capturedJob.getClientId());
        assertEquals(Job.ScheduleType.ONE_TIME, capturedJob.getScheduleType());
        assertEquals(Job.JobStatus.SCHEDULING, capturedJob.getStatus());
        assertEquals("UTC", capturedJob.getTimeZone());
    }

    @Test
    void deleteJobAsync_ShouldDelegateToJobSchedulerService() throws ExecutionException, InterruptedException {
        // Arrange
        doNothing().when(jobSchedulerService).deleteJob(jobId);
        
        // Act
        CompletableFuture<Void> future = asyncJobService.deleteJobAsync(jobId);
        future.get(); // Wait for the async operation to complete
        
        // Assert
        verify(jobSchedulerService).deleteJob(jobId);
    }

    @Test
    void pauseJobAsync_ShouldDelegateToJobSchedulerService() throws ExecutionException, InterruptedException {
        // Arrange
        when(jobSchedulerService.pauseJob(jobId)).thenReturn(jobResponse);
        
        // Act
        CompletableFuture<JobResponse> future = asyncJobService.pauseJobAsync(jobId);
        JobResponse result = future.get(); // Wait for the async operation to complete
        
        // Assert
        assertNotNull(result);
        assertEquals(jobId, result.getId());
        verify(jobSchedulerService).pauseJob(jobId);
    }

    @Test
    void resumeJobAsync_ShouldDelegateToJobSchedulerService() throws ExecutionException, InterruptedException {
        // Arrange
        when(jobSchedulerService.resumeJob(jobId)).thenReturn(jobResponse);
        
        // Act
        CompletableFuture<JobResponse> future = asyncJobService.resumeJobAsync(jobId);
        JobResponse result = future.get(); // Wait for the async operation to complete
        
        // Assert
        assertNotNull(result);
        assertEquals(jobId, result.getId());
        verify(jobSchedulerService).resumeJob(jobId);
    }
}