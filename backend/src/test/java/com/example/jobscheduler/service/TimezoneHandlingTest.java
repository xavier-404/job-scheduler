package com.example.jobscheduler.service;

import com.example.jobscheduler.dto.JobRequest;
import com.example.jobscheduler.dto.JobResponse;
import com.example.jobscheduler.exception.InvalidTimeZoneException;
import com.example.jobscheduler.exception.PastScheduleTimeException;
import com.example.jobscheduler.model.Job;
import com.example.jobscheduler.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Scheduler;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TimezoneHandlingTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private Scheduler scheduler;

    @InjectMocks
    private JobSchedulerService jobSchedulerService;

    private JobRequest jobRequest;
    private Job job;

    @BeforeEach
    void setUp() {
        // Initialize valid timezones for testing
        ReflectionTestUtils.setField(jobSchedulerService, "validTimeZones", ZoneId.getAvailableZoneIds());
        
        // Create a common job request for tests
        jobRequest = JobRequest.builder()
                .clientId("TEST_CLIENT")
                .scheduleType(Job.ScheduleType.ONE_TIME)
                .timeZone("America/New_York")
                .build();
        
        // Create a common job for tests
        job = Job.builder()
                .id(UUID.randomUUID())
                .clientId("TEST_CLIENT")
                .scheduleType(Job.ScheduleType.ONE_TIME)
                .timeZone("America/New_York")
                .status(Job.JobStatus.SCHEDULED)
                .build();
        
        // Mock common behavior
        when(jobRepository.save(any(Job.class))).thenReturn(job);
    }

    @Test
    void shouldAcceptValidTimezone() {
        // Arrange
        jobRequest.setStartTime(LocalDateTime.now().plusHours(1));
        
        // Act & Assert
        assertDoesNotThrow(() -> jobSchedulerService.createJob(jobRequest));
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void shouldRejectInvalidTimezone() {
        // Arrange
        jobRequest.setTimeZone("Invalid/Timezone");
        jobRequest.setStartTime(LocalDateTime.now().plusHours(1));
        
        // Act & Assert
        assertThrows(InvalidTimeZoneException.class, () -> jobSchedulerService.createJob(jobRequest));
    }

    @Test
    void shouldUseUtcAsDefaultWhenTimezoneIsNull() {
        // Arrange
        jobRequest.setTimeZone(null);
        jobRequest.setStartTime(LocalDateTime.now().plusHours(1));
        
        // Act
        jobSchedulerService.createJob(jobRequest);
        
        // Assert
        verify(jobRepository).save(Mockito.argThat(savedJob -> 
            "UTC".equals(savedJob.getTimeZone())
        ));
    }

    @Test
    void shouldRejectPastTimeInSpecifiedTimezone() {
        // Arrange - create a time that's in the past in the specified timezone
        ZoneId nyZone = ZoneId.of("America/New_York");
        LocalDateTime pastTimeInNY = LocalDateTime.now(nyZone).minusHours(1);
        jobRequest.setStartTime(pastTimeInNY);
        
        // Act & Assert
        assertThrows(PastScheduleTimeException.class, () -> jobSchedulerService.createJob(jobRequest));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Asia/Tokyo", "Europe/London", "Australia/Sydney"})
    void shouldHandleVariousTimezones(String timezone) {
        // Arrange
        jobRequest.setTimeZone(timezone);
        
        // Set start time to be 1 hour in the future in the specific timezone
        ZoneId zoneId = ZoneId.of(timezone);
        ZonedDateTime futureTimeInZone = ZonedDateTime.now(zoneId).plusHours(1);
        jobRequest.setStartTime(futureTimeInZone.toLocalDateTime());
        
        // Configure the mock to return a job with the correct timezone
        Job jobWithSpecificTimezone = Job.builder()
                .id(UUID.randomUUID())
                .clientId("TEST_CLIENT")
                .scheduleType(Job.ScheduleType.ONE_TIME)
                .timeZone(timezone)
                .status(Job.JobStatus.SCHEDULED)
                .build();
        when(jobRepository.save(any(Job.class))).thenReturn(jobWithSpecificTimezone);
        
        // Act
        JobResponse response = jobSchedulerService.createJob(jobRequest);
        
        // Assert
        assertEquals(timezone, response.getTimeZone());
        verify(jobRepository).save(Mockito.argThat(savedJob -> 
            timezone.equals(savedJob.getTimeZone())
        ));
    }
}