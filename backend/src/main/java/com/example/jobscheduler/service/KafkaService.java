package com.example.jobscheduler.service;

import com.example.jobscheduler.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for interacting with Kafka.
 * Handles publishing user data to Kafka topics.
 */
@Service // Marks this class as a Spring service component.
@Slf4j // Lombok annotation to enable logging in this class.
@RequiredArgsConstructor // Lombok annotation to generate a constructor for all final fields.
public class KafkaService {

    private final KafkaTemplate<String, String> kafkaTemplate; // KafkaTemplate for sending messages to Kafka.
    private final ObjectMapper objectMapper; // ObjectMapper for serializing objects to JSON.

    @Value("${kafka.topic.user-data}") // Injects the Kafka topic name from the application properties.
    private String userDataTopic; // Name of the Kafka topic for user data.

    /**
     * Publishes a user to the Kafka topic.
     *
     * @param user the user to publish
     * @return a CompletableFuture that completes when the message is published
     * @throws JsonProcessingException if the user cannot be serialized to JSON
     */
    public CompletableFuture<SendResult<String, String>> publishUser(User user) throws JsonProcessingException {
        // Serialize the User object to a JSON string.
        String userJson = objectMapper.writeValueAsString(user);

        // Generate a unique key for the Kafka message using the clientId and userId.
        String key = user.getClientId() + "-" + user.getId();

        // Send the message to the Kafka topic asynchronously.
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(userDataTopic, key, userJson);

        // Add a callback to handle success or failure of the message publishing.
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                // Log an error if the message fails to publish.
                log.error("Failed to publish user {} to Kafka: {}", user.getId(), ex.getMessage(), ex);
            } else {
                // Log success details if the message is published successfully.
                log.info("User {} published to Kafka topic {}, partition {}, offset {}",
                        user.getId(), // User ID.
                        result.getRecordMetadata().topic(), // Kafka topic name.
                        result.getRecordMetadata().partition(), // Partition where the message was published.
                        result.getRecordMetadata().offset()); // Offset of the message in the partition.
            }
        });

        // Return the CompletableFuture to allow the caller to handle the result.
        return future;
    }
}