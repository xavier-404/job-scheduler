package com.example.jobscheduler.integration;

import com.example.jobscheduler.dto.JobRequest;
import com.example.jobscheduler.dto.JobResponse;
import com.example.jobscheduler.model.Job;
import com.example.jobscheduler.model.User;
import com.example.jobscheduler.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for the Job Scheduler application.
 * Uses TestContainers to create ephemeral Kafka and PostgreSQL instances.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class JobSchedulerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:14-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    /**
     * Dynamically sets Spring Boot properties to use TestContainers.
     */
    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    /**
     * Tests creating a job and verifies it's created successfully.
     */
    @Test
    public void testCreateAndRetrieveJob() {
        // Arrange
        String clientId = "TEST_CLIENT_" + UUID.randomUUID().toString().substring(0, 8);
        
        // Create a test user with the client ID
        User user = User.builder()
                .clientId(clientId)
                .name("Test User")
                .email("test@example.com")
                .address("Test Address")
                .phone("555-123-4567")
                .build();
        
        userRepository.save(user);
        
        // Create a one-time job request
        JobRequest jobRequest = JobRequest.builder()
                .clientId(clientId)
                .scheduleType(Job.ScheduleType.ONE_TIME)
                .startTime(LocalDateTime.now().plusMinutes(5))
                .timeZone("UTC")
                .build();
        
        // Act
        ResponseEntity<JobResponse> createResponse = restTemplate.postForEntity(
                "/api/jobs",
                jobRequest,
                JobResponse.class
        );
        
        // Assert
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        JobResponse createdJob = createResponse.getBody();
        assertNotNull(createdJob);
        assertNotNull(createdJob.getId());
        assertEquals(clientId, createdJob.getClientId());
        assertEquals(Job.JobStatus.SCHEDULED, createdJob.getStatus());
        
        // Verify we can retrieve the job
        ResponseEntity<JobResponse> retrieveResponse = restTemplate.getForEntity(
                "/api/jobs/" + createdJob.getId(),
                JobResponse.class
        );
        
        assertEquals(HttpStatus.OK, retrieveResponse.getStatusCode());
        JobResponse retrievedJob = retrieveResponse.getBody();
        assertNotNull(retrievedJob);
        assertEquals(createdJob.getId(), retrievedJob.getId());
        assertEquals(clientId, retrievedJob.getClientId());
    }
}