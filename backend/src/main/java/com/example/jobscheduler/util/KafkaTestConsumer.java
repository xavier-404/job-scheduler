package com.example.jobscheduler.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Utility component that consumes messages from Kafka for testing and monitoring purposes.
 * It stores received messages in memory for inspection via REST API.
 */
@Component
@Slf4j
public class KafkaTestConsumer {

    private final List<String> receivedMessages = new CopyOnWriteArrayList<>();
    private static final int MAX_MESSAGES = 100;

    /**
     * Listens for messages on the user-data Kafka topic.
     * Uses ConcurrentKafkaListenerContainerFactory to handle messages concurrently.
     *
     * @param message the received message
     */
    @KafkaListener(
        topics = "${kafka.topic.user-data}", 
        groupId = "test-consumer",
        concurrency = "3" // Process messages from multiple partitions concurrently
    )
    public void listen(String message) {
        log.info("Received Kafka message: {}", message);
        
        // Add message to list and keep only the most recent messages
        receivedMessages.add(message);
        if (receivedMessages.size() > MAX_MESSAGES) {
            receivedMessages.remove(0);
        }
    }
    
    /**
     * Gets the list of received messages.
     *
     * @return a copy of the received messages list
     */
    public List<String> getReceivedMessages() {
        return new ArrayList<>(receivedMessages);
    }
    
    /**
     * Clears the received messages list.
     */
    public void clearMessages() {
        log.info("Clearing received messages list");
        receivedMessages.clear();
    }
}