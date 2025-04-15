// Save as: backend/src/test/java/com/example/jobscheduler/integration/TimezoneIntegrationTest.java

package com.example.jobscheduler.integration;

import com.example.jobscheduler.dto.JobRequest;
import com.example.jobscheduler.dto.JobResponse;
import com.example.jobscheduler.model.Job;
import com.example.jobscheduler.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class TimezoneIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:14-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        // Use in-memory Kafka for this test
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
    }
    
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JobRepository jobRepository;
    
    private String testClientId;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    
    @BeforeEach
    void setUp() {
        // Clear test data
        jobRepository.deleteAll();
        testClientId = "TZ_TEST_" + UUID.randomUUID().toString().substring(0, 8);
    }

    @ParameterizedTest
    @ValueSource(strings = {"UTC", "Asia/Kolkata", "America/New_York", "Europe/London", "Australia/Sydney"})
    public void shouldHandleTimezoneCorrectly(String timezone) {
        // Given
        ZoneId zoneId = ZoneId.of(timezone);
        LocalDateTime futureTimeInTz = LocalDateTime.now(zoneId).plusHours(1);
        String formattedTime = futureTimeInTz.format(formatter);
        
        // Create a job request
        JobRequest jobRequest = JobRequest.builder()
                .clientId(testClientId)
                .scheduleType(Job.ScheduleType.ONE_TIME)
                .startTime(futureTimeInTz)
                .timeZone(timezone)
                .build();
        
        // When
        ResponseEntity<JobResponse> response = restTemplate.postForEntity(
                "/api/jobs",
                jobRequest,
                JobResponse.class
        );
        
        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        JobResponse createdJob = response.getBody();
        assertNotNull(createdJob);
        assertEquals(timezone, createdJob.getTimeZone());
        
        // Verify the stored time matches the input time
        Job savedJob = jobRepository.findById(createdJob.getId()).orElse(null);
        assertNotNull(savedJob);
        assertEquals(timezone, savedJob.getTimeZone());
        
        // The startTime should be formatted the same as the input time
        // Even though internally it may be stored differently
        String savedTimeStr = savedJob.getStartTime().format(formatter);
        assertEquals(formattedTime, savedTimeStr);
    }
}