package com.example.jobscheduler.service;

import com.example.jobscheduler.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of MessagePublisher that uses Kafka as the messaging system.
 * Handles publishing user data to Kafka topics.
 */
@Service
@Slf4j
public class KafkaService implements MessagePublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String userDataTopic;

    /**
     * Constructor with dependency injection.
     * Using constructor injection rather than field injection for better testability.
     *
     * @param kafkaTemplate the KafkaTemplate for sending messages to Kafka
     * @param objectMapper the ObjectMapper for serializing objects to JSON
     * @param userDataTopic the name of the Kafka topic for user data
     */
    public KafkaService(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${kafka.topic.user-data}") String userDataTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.userDataTopic = userDataTopic;
        
        log.info("Initialized KafkaService with user data topic: {}", userDataTopic);
    }

    /**
     * Publishes a user to the Kafka topic.
     * Implements automatic retry with exponential backoff.
     *
     * @param user the user to publish
     * @return a CompletableFuture that completes when the message is published
     * @throws JsonProcessingException if the user cannot be serialized to JSON
     */
    @Override
    @Retryable(
        value = { Exception.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public CompletableFuture<SendResult<String, String>> publishUser(User user) throws JsonProcessingException {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        // Serialize the User object to a JSON string
        String userJson = objectMapper.writeValueAsString(user);
        
        // Generate a unique key for the Kafka message using the clientId and userId
        String key = user.getClientId() + "-" + user.getId();
        
        log.debug("Preparing to publish user {} for client {} to Kafka", 
                user.getId(), user.getClientId());
        
        // Send the message to the Kafka topic asynchronously
        CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(userDataTopic, key, userJson);
        
        // Add a callback to handle success or failure of the message publishing
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                // Log an error if the message fails to publish
                log.error("Failed to publish user {} for client {} to Kafka: {}", 
                        user.getId(), user.getClientId(), ex.getMessage(), ex);
            } else {
                // Log success details if the message is published successfully
                log.info("User {} for client {} published to Kafka topic {}, partition {}, offset {}",
                        user.getId(),
                        user.getClientId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
        
        return future;
    }
}