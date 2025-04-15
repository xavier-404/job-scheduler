// Save as: src/test/java/com/example/jobscheduler/JobSchedulerApplicationTests.java

package com.example.jobscheduler;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.quartz.Scheduler;

@SpringBootTest(classes = JobSchedulerApplication.class)
@ActiveProfiles("test")
class JobSchedulerApplicationTests {

    // Mock the main dependencies that are causing context loading issues
    @MockBean
    private Scheduler scheduler;
    
    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void contextLoads() {
        // This test will just verify the Spring context loads successfully
    }
}