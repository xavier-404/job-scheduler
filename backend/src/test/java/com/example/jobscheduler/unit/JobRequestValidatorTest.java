// Save as: backend/src/test/java/com/example/jobscheduler/unit/JobRequestValidatorTest.java

package com.example.jobscheduler.unit;

import com.example.jobscheduler.dto.JobRequest;
import com.example.jobscheduler.exception.InvalidTimeZoneException;
import com.example.jobscheduler.exception.PastScheduleTimeException;
import com.example.jobscheduler.model.Job;
import com.example.jobscheduler.service.JobSchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Scheduler;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class JobRequestValidatorTest {

    @Mock
    private Scheduler scheduler;

    @InjectMocks
    private JobSchedulerService jobSchedulerService;

    @BeforeEach
    void setUp() {
        Set<String> validTimeZones = new HashSet<>(ZoneId.getAvailableZoneIds());
        ReflectionTestUtils.setField(jobSchedulerService, "validTimeZones", validTimeZones);
    }

    @Test
    void shouldThrowExceptionForInvalidTimezone() {
        // Given
        JobRequest jobRequest = JobRequest.builder()
                .clientId("TEST_CLIENT")
                .scheduleType(Job.ScheduleType.ONE_TIME)
                .timeZone("Invalid/Timezone")
                .startTime(LocalDateTime.now().plusHours(1))
                .build();

        // When & Then
        assertThrows(InvalidTimeZoneException.class, () -> {
            ReflectionTestUtils.invokeMethod(
                    jobSchedulerService,
                    "validateTimeZone",
                    jobRequest.getTimeZone());
        });
    }

    @Test
    void shouldThrowExceptionForPastTime() {
        // Given
        // Use a time VERY far in the past to ensure it triggers the exception
        LocalDateTime pastTime = LocalDateTime.now().minusHours(24); // A full day in the past
        
        // Removed unused local variable "job"
    
        // When & Then
        PastScheduleTimeException exception = assertThrows(PastScheduleTimeException.class, () -> {
            // Use direct method invocation instead of reflection for clearer error messages
            jobSchedulerService.createJob(JobRequest.builder()
                    .clientId("TEST_CLIENT")
                    .scheduleType(Job.ScheduleType.ONE_TIME)
                    .timeZone("UTC")
                    .startTime(pastTime)
                    .build());
        });
        
        // Verify exception message contains expected text
        assertTrue(exception.getMessage().contains("Cannot schedule job in the past"));
    }

    @Test
    void shouldAcceptFutureTime() {
        // Given
        LocalDateTime futureTime = LocalDateTime.now().plusHours(1);
        Job job = Job.builder()
                .scheduleType(Job.ScheduleType.ONE_TIME)
                .timeZone("UTC")
                .startTime(futureTime)
                .build();

        // When & Then
        assertDoesNotThrow(() -> {
            ReflectionTestUtils.invokeMethod(
                    jobSchedulerService,
                    "validateJobTiming",
                    job);
        });
    }
}