package com.example.jobscheduler.controller;

import com.example.jobscheduler.util.KafkaTestConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final KafkaTestConsumer kafkaTestConsumer;
    
    @GetMapping("/kafka-messages")
    public List<String> getKafkaMessages() {
        return kafkaTestConsumer.getReceivedMessages();
    }
    
    @PostMapping("/clear-messages")
    public void clearMessages() {
        kafkaTestConsumer.clearMessages();
    }
}