// Save as: backend/src/test/java/com/example/jobscheduler/unit/JobSchedulerServiceTest.java

package com.example.jobscheduler.unit;

import com.example.jobscheduler.dto.JobRequest;
import com.example.jobscheduler.dto.JobResponse;
import com.example.jobscheduler.model.Job;
import com.example.jobscheduler.repository.JobRepository;
import com.example.jobscheduler.service.JobSchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.mockito.MockedStatic;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JobSchedulerServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private Scheduler scheduler;

    @InjectMocks
    private JobSchedulerService jobSchedulerService;

    private UUID testJobId;
    private Job mockJob;

    @BeforeEach
    void setUp() {
        testJobId = UUID.randomUUID();
        mockJob = Job.builder()
                .id(testJobId)
                .clientId("TEST_CLIENT")
                .scheduleType(Job.ScheduleType.ONE_TIME)
                .timeZone("UTC")
                .status(Job.JobStatus.SCHEDULED)
                .build();

        Set<String> validTimeZones = new HashSet<>(ZoneId.getAvailableZoneIds());
        ReflectionTestUtils.setField(jobSchedulerService, "validTimeZones", validTimeZones);

        // Remove this from setup
        // when(jobRepository.save(any(Job.class))).thenReturn(mockJob);
    }

    // Modify the JobSchedulerServiceTest.java file

    @Test
    void shouldCreateOneTimeJob() throws SchedulerException {
        // Given
        when(jobRepository.save(any(Job.class))).thenReturn(mockJob);

        JobRequest jobRequest = JobRequest.builder()
                .clientId("TEST_CLIENT")
                .scheduleType(Job.ScheduleType.ONE_TIME)
                .timeZone("UTC")
                .startTime(LocalDateTime.now().plusHours(1))
                .build();

        // IMPORTANT: Mock TransactionSynchronizationManager
        MockedStatic<TransactionSynchronizationManager> mockedStatic = Mockito
                .mockStatic(TransactionSynchronizationManager.class);

        try {
            // Make it appear that transaction synchronization is active
            mockedStatic.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            // Capture the TransactionSynchronization that gets registered
            mockedStatic
                    .when(() -> TransactionSynchronizationManager
                            .registerSynchronization(any(TransactionSynchronization.class)))
                    .thenAnswer(invocation -> {
                        // Get the synchronization object
                        TransactionSynchronization sync = invocation.getArgument(0);

                        // Execute the afterCommit method directly
                        sync.afterCommit();

                        return null;
                    });

            // Mock the trigger
            Trigger mockTrigger = mock(Trigger.class);
            when(mockTrigger.getNextFireTime()).thenReturn(new Date());
            when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class))).thenReturn(new Date());

            // When
            JobResponse response = jobSchedulerService.createJob(jobRequest);

            // Then
            assertNotNull(response);
            assertEquals(testJobId, response.getId());
            assertEquals("TEST_CLIENT", response.getClientId());
            assertEquals(Job.JobStatus.SCHEDULED, response.getStatus());

            // Verify
            verify(jobRepository).save(any(Job.class));
            verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
        } finally {
            // Always close the mocked static
            mockedStatic.close();
        }
    }

    @Test
    void shouldCreateRecurringJob() throws SchedulerException {
        // Given
        when(jobRepository.save(any(Job.class))).thenReturn(mockJob);

        JobRequest jobRequest = JobRequest.builder()
                .clientId("TEST_CLIENT")
                .scheduleType(Job.ScheduleType.RECURRING)
                .timeZone("UTC")
                .daysOfWeek(List.of(1, 3, 5)) // Mon, Wed, Fri
                .recurringTimeHour(9)
                .recurringTimeMinute(0)
                .build();

        // IMPORTANT: Mock TransactionSynchronizationManager
        MockedStatic<TransactionSynchronizationManager> mockedStatic = Mockito
                .mockStatic(TransactionSynchronizationManager.class);

        try {
            // Make it appear that transaction synchronization is active
            mockedStatic.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            // Capture the TransactionSynchronization that gets registered
            mockedStatic
                    .when(() -> TransactionSynchronizationManager
                            .registerSynchronization(any(TransactionSynchronization.class)))
                    .thenAnswer(invocation -> {
                        // Get the synchronization object
                        TransactionSynchronization sync = invocation.getArgument(0);

                        // Execute the afterCommit method directly
                        sync.afterCommit();

                        return null;
                    });

            // Mock the trigger
            Trigger mockTrigger = mock(Trigger.class);
            when(mockTrigger.getNextFireTime()).thenReturn(new Date());
            when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class))).thenReturn(new Date());

            // When
            JobResponse response = jobSchedulerService.createJob(jobRequest);

            // Then
            assertNotNull(response);
            assertEquals(testJobId, response.getId());
            assertEquals("TEST_CLIENT", response.getClientId());
            assertEquals(Job.JobStatus.SCHEDULED, response.getStatus());

            // Verify
            verify(jobRepository).save(any(Job.class));
            verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
        } finally {
            // Always close the mocked static
            mockedStatic.close();
        }
    }

    @Test
    void shouldGetJobById() {
        // Given
        when(jobRepository.findById(testJobId)).thenReturn(Optional.of(mockJob));

        // When
        JobResponse response = jobSchedulerService.getJobById(testJobId);

        // Then
        assertNotNull(response);
        assertEquals(testJobId, response.getId());
        assertEquals("TEST_CLIENT", response.getClientId());
    }

    @Test
    void shouldGetAllJobs() {
        // Given
        Job job1 = Job.builder().id(UUID.randomUUID()).clientId("CLIENT1").build();
        Job job2 = Job.builder().id(UUID.randomUUID()).clientId("CLIENT2").build();
        when(jobRepository.findAll()).thenReturn(List.of(job1, job2));

        // When
        List<JobResponse> responses = jobSchedulerService.getAllJobs();

        // Then
        assertEquals(2, responses.size());
        assertEquals("CLIENT1", responses.get(0).getClientId());
        assertEquals("CLIENT2", responses.get(1).getClientId());
    }
}