package com.safepe.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safepe.payment.config.KafkaConfig;
import com.safepe.payment.dto.TransactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PaymentEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void publishTransactionEvent(TransactionEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaConfig.TRANSACTION_EVENTS_TOPIC,
                    event.getTransactionId().toString(), payload);
            log.info("📤 Published TransactionEvent to Kafka: txn={}, amount=₹{}, upi={}",
                    event.getTransactionId(), event.getAmount(), event.getUpiId());
        } catch (JsonProcessingException e) {
            log.error("❌ Failed to serialize TransactionEvent for Kafka: {}", e.getMessage());
        }
    }
}
