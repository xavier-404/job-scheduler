// Save as: backend/src/test/java/com/example/jobscheduler/integration/JobSchedulerIntegrationTest.java

package com.example.jobscheduler.integration;

import com.example.jobscheduler.dto.JobRequest;
import com.example.jobscheduler.dto.JobResponse;
import com.example.jobscheduler.model.Job;
import com.example.jobscheduler.model.User;
import com.example.jobscheduler.repository.JobRepository;
import com.example.jobscheduler.repository.UserRepository;
import com.example.jobscheduler.util.KafkaTestConsumer;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

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
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private KafkaTestConsumer kafkaTestConsumer;

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }
    
    private String testClientId;
    
    @BeforeEach
    void setUp() {
        // Clear test data
        jobRepository.deleteAll();
        
        testClientId = "TEST_CLIENT_" + UUID.randomUUID().toString().substring(0, 8);
        
        // Create test users
        User user1 = User.builder()
                .clientId(testClientId)
                .name("Test User 1")
                .email("test1@example.com")
                .build();
                
        User user2 = User.builder()
                .clientId(testClientId)
                .name("Test User 2")
                .email("test2@example.com")
                .build();
                
        userRepository.save(user1);
        userRepository.save(user2);
        
        // Clear Kafka messages
        kafkaTestConsumer.clearMessages();
    }

    @Test
    public void testCreateAndRetrieveJob() {
        // Create a one-time job request
        JobRequest jobRequest = JobRequest.builder()
                .clientId(testClientId)
                .scheduleType(Job.ScheduleType.ONE_TIME)
                .startTime(LocalDateTime.now().plusMinutes(5))
                .timeZone("UTC")
                .build();
        
        // Submit the job
        ResponseEntity<JobResponse> createResponse = restTemplate.postForEntity(
                "/api/jobs",
                jobRequest,
                JobResponse.class
        );
        
        // Verify created successfully
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        JobResponse createdJob = createResponse.getBody();
        assertNotNull(createdJob);
        assertNotNull(createdJob.getId());
        assertEquals(testClientId, createdJob.getClientId());
        assertEquals(Job.JobStatus.SCHEDULED, createdJob.getStatus());
        
        // Verify retrieval works
        ResponseEntity<JobResponse> retrieveResponse = restTemplate.getForEntity(
                "/api/jobs/" + createdJob.getId(),
                JobResponse.class
        );
        
        assertEquals(HttpStatus.OK, retrieveResponse.getStatusCode());
        JobResponse retrievedJob = retrieveResponse.getBody();
        assertNotNull(retrievedJob);
        assertEquals(createdJob.getId(), retrievedJob.getId());
    }
    
    @Test
    public void testImmediateJobExecution() {
        // Create an immediate execution job
        JobRequest jobRequest = JobRequest.builder()
                .clientId(testClientId)
                .scheduleType(Job.ScheduleType.IMMEDIATE)
                .timeZone("UTC")
                .build();
        
        // Submit the job
        ResponseEntity<JobResponse> createResponse = restTemplate.postForEntity(
                "/api/jobs",
                jobRequest,
                JobResponse.class
        );
        
        // Verify created successfully
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        JobResponse createdJob = createResponse.getBody();
        assertNotNull(createdJob);
        
        // Wait for job to execute and complete
        String jobId = createdJob.getId().toString();
        await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .until(() -> {
                ResponseEntity<JobResponse> response = restTemplate.getForEntity(
                    "/api/jobs/" + jobId,
                    JobResponse.class
                );
                JobResponse job = response.getBody();
                return job != null && 
                      (job.getStatus() == Job.JobStatus.COMPLETED_SUCCESS || 
                       job.getStatus() == Job.JobStatus.COMPLETED_FAILURE);
            });
            
        // Verify users were published to Kafka
        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> assertTrue(kafkaTestConsumer.getReceivedMessages().size() >= 2));
            
        List<String> messages = kafkaTestConsumer.getReceivedMessages();
        assertTrue(messages.size() >= 2, "Should have published at least 2 user messages");
        
        // Verify the messages contain our test users
        boolean foundUser1 = false;
        boolean foundUser2 = false;
        
        for (String message : messages) {
            if (message.contains("test1@example.com")) {
                foundUser1 = true;
            }
            if (message.contains("test2@example.com")) {
                foundUser2 = true;
            }
        }
        
        assertTrue(foundUser1, "User 1 should be published to Kafka");
        assertTrue(foundUser2, "User 2 should be published to Kafka");
    }
    
    @Test
    public void testTimezoneHandling() {
        // Test with different timezones
        String[] testTimezones = {"UTC", "Asia/Kolkata", "America/New_York"};
        
        for (String timezone : testTimezones) {
            // Get current time in the test timezone
            ZoneId zoneId = ZoneId.of(timezone);
            LocalDateTime futureTimeInTz = LocalDateTime.now(zoneId).plusMinutes(10);
            
            // Create a job request
            JobRequest jobRequest = JobRequest.builder()
                    .clientId(testClientId)
                    .scheduleType(Job.ScheduleType.ONE_TIME)
                    .startTime(futureTimeInTz)
                    .timeZone(timezone)
                    .build();
            
            // Submit the job
            ResponseEntity<JobResponse> response = restTemplate.postForEntity(
                    "/api/jobs",
                    jobRequest,
                    JobResponse.class
            );
            
            // Verify
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            JobResponse createdJob = response.getBody();
            assertNotNull(createdJob);
            assertEquals(timezone, createdJob.getTimeZone());
            
            // Get job from DB to verify timezone was stored correctly
            Job savedJob = jobRepository.findById(createdJob.getId()).orElse(null);
            assertNotNull(savedJob);
            assertEquals(timezone, savedJob.getTimeZone());
        }
    }
}