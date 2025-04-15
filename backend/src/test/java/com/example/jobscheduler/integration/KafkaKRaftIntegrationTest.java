package com.example.jobscheduler.integration;

import com.example.jobscheduler.model.User;
import com.example.jobscheduler.service.KafkaService;
import com.example.jobscheduler.util.KafkaTestConsumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Kafka in KRaft mode.
 * Uses an embedded Kafka broker to test the publishing and consuming of messages.
 */
@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092",
        "auto.create.topics.enable=true",
        "transaction.state.log.replication.factor=1",
        "offsets.topic.replication.factor=1",
        // KRaft specific properties
        "process.roles=broker,controller",
        "node.id=1",
        "controller.quorum.voters=1@localhost:9093"
    }
)
public class KafkaKRaftIntegrationTest {

    @Autowired
    private KafkaService kafkaService;
    
    @Autowired
    private KafkaTestConsumer kafkaTestConsumer;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        // Clear any existing messages
        kafkaTestConsumer.clearMessages();
        
        // Create a test user
        testUser = User.builder()
                .id(UUID.randomUUID())
                .clientId("TEST_CLIENT")
                .name("Test User")
                .email("test@example.com")
                .address("123 Test St")
                .phone("555-123-4567")
                .build();
    }
    
    @AfterEach
    void tearDown() {
        kafkaTestConsumer.clearMessages();
    }
    
    @Test
    void shouldPublishAndConsumeMessage() throws Exception {
        // Publish a user to Kafka
        kafkaService.publishUser(testUser).get(10, TimeUnit.SECONDS);
        
        // Wait for the message to be consumed by the test consumer
        await().atMost(10, TimeUnit.SECONDS)
               .untilAsserted(() -> {
                   List<String> messages = kafkaTestConsumer.getReceivedMessages();
                   assertFalse(messages.isEmpty(), "No messages received");
                   
                   // Verify the message contents
                   String receivedJson = messages.get(0);
                   User receivedUser = objectMapper.readValue(receivedJson, User.class);
                   
                   assertEquals(testUser.getId(), receivedUser.getId());
                   assertEquals(testUser.getClientId(), receivedUser.getClientId());
                   assertEquals(testUser.getName(), receivedUser.getName());
                   assertEquals(testUser.getEmail(), receivedUser.getEmail());
               });
    }
    
    @Test
    void shouldHandleMultipleMessages() throws Exception {
        // Create multiple users
        User user1 = User.builder()
                .id(UUID.randomUUID())
                .clientId("CLIENT_1")
                .name("User 1")
                .email("user1@example.com")
                .build();
        
        User user2 = User.builder()
                .id(UUID.randomUUID())
                .clientId("CLIENT_2")
                .name("User 2")
                .email("user2@example.com")
                .build();
        
        // Publish both users to Kafka
        kafkaService.publishUser(user1).get(10, TimeUnit.SECONDS);
        kafkaService.publishUser(user2).get(10, TimeUnit.SECONDS);
        
        // Wait for both messages to be consumed
        await().atMost(10, TimeUnit.SECONDS)
               .untilAsserted(() -> {
                   List<String> messages = kafkaTestConsumer.getReceivedMessages();
                   assertTrue(messages.size() >= 2, "Expected at least 2 messages, got: " + messages.size());
                   
                   // Check if both users are present in the received messages
                   boolean foundUser1 = false;
                   boolean foundUser2 = false;
                   
                   for (String message : messages) {
                       User receivedUser = objectMapper.readValue(message, User.class);
                       if (user1.getId().equals(receivedUser.getId())) {
                           foundUser1 = true;
                       } else if (user2.getId().equals(receivedUser.getId())) {
                           foundUser2 = true;
                       }
                   }
                   
                   assertTrue(foundUser1, "User 1 not found in received messages");
                   assertTrue(foundUser2, "User 2 not found in received messages");
               });
    }
}