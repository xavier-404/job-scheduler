// Save as: backend/src/test/java/com/example/jobscheduler/unit/AsyncJobServiceTest.java

package com.example.jobscheduler.unit;

import com.example.jobscheduler.dto.JobRequest;
import com.example.jobscheduler.dto.JobResponse;
import com.example.jobscheduler.model.Job;
import com.example.jobscheduler.repository.JobRepository;
import com.example.jobscheduler.service.AsyncJobService;
import com.example.jobscheduler.service.JobSchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private UUID jobId;

    @BeforeEach
    void setUp() {
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

        // Only set up the mock for the scheduleJobAsync test
        // Remove this from setUp and move it to each individual test
        // when(jobSchedulerService.createJob(any(JobRequest.class))).thenReturn(jobResponse);
    }

    @Test
    void scheduleJobAsync_ShouldDelegateToJobSchedulerService() throws ExecutionException, InterruptedException {
        // Setup mock specifically for this test
        when(jobSchedulerService.createJob(any(JobRequest.class))).thenReturn(jobResponse);
        
        // When
        CompletableFuture<JobResponse> future = asyncJobService.scheduleJobAsync(jobRequest);
        JobResponse result = future.get();

        // Then
        assertNotNull(result);
        assertEquals(jobId, result.getId());
        assertEquals("TEST_CLIENT", result.getClientId());
        verify(jobSchedulerService).createJob(jobRequest);

    }

    @Test
    void deleteJobAsync_ShouldDelegateToJobSchedulerService() throws ExecutionException, InterruptedException {
        // Given
        doNothing().when(jobSchedulerService).deleteJob(jobId);

        // When
        CompletableFuture<Void> future = asyncJobService.deleteJobAsync(jobId);
        future.get();

        // Then
        verify(jobSchedulerService).deleteJob(jobId);
    }

    @Test
    void pauseJobAsync_ShouldDelegateToJobSchedulerService() throws ExecutionException, InterruptedException {
        // Given
        when(jobSchedulerService.pauseJob(jobId)).thenReturn(jobResponse);

        // When
        CompletableFuture<JobResponse> future = asyncJobService.pauseJobAsync(jobId);
        JobResponse result = future.get();

        // Then
        assertNotNull(result);
        assertEquals(jobId, result.getId());
        verify(jobSchedulerService).pauseJob(jobId);
    }

    @Test
    void resumeJobAsync_ShouldDelegateToJobSchedulerService() throws ExecutionException, InterruptedException {
        // Given
        when(jobSchedulerService.resumeJob(jobId)).thenReturn(jobResponse);

        // When
        CompletableFuture<JobResponse> future = asyncJobService.resumeJobAsync(jobId);
        JobResponse result = future.get();

        // Then
        assertNotNull(result);
        assertEquals(jobId, result.getId());
        verify(jobSchedulerService).resumeJob(jobId);
    }
}