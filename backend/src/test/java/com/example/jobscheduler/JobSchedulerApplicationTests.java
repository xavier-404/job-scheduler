package com.example.jobscheduler;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.quartz.Scheduler;

@SpringBootTest
@ActiveProfiles("test")
class JobSchedulerApplicationTests {

    // Mock all external dependencies that might cause context loading issues
    @MockBean
    private Scheduler scheduler;
    
    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void contextLoads() {
        // This test will simply verify the Spring context loads successfully
    }
}