package com.example.jobscheduler.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class KafkaTestConsumer {

    private final List<String> receivedMessages = new CopyOnWriteArrayList<>();
    private static final int MAX_MESSAGES = 100;

    @KafkaListener(topics = "${kafka.topic.user-data}", groupId = "test-consumer")
    public void listen(String message) {
        log.info("Received Kafka message: {}", message);
        
        // Add message to list and keep only the most recent messages
        receivedMessages.add(message);
        if (receivedMessages.size() > MAX_MESSAGES) {
            receivedMessages.remove(0);
        }
    }
    
    public List<String> getReceivedMessages() {
        return new ArrayList<>(receivedMessages);
    }
    
    public void clearMessages() {
        receivedMessages.clear();
    }
}