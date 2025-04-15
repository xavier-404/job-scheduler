package com.example.jobscheduler.service;

import com.example.jobscheduler.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for message publishing services.
 * Defines the contract for publishing messages to messaging systems like Kafka.
 */
public interface MessagePublisher {

    /**
     * Publishes a user to the messaging system.
     *
     * @param user the user to publish
     * @return a CompletableFuture that completes when the message is published
     * @throws JsonProcessingException if the user cannot be serialized to JSON
     */
    CompletableFuture<SendResult<String, String>> publishUser(User user) throws JsonProcessingException;
}