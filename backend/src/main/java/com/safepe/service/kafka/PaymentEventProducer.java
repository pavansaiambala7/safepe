package com.safepe.service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safepe.config.KafkaConfig;
import com.safepe.dto.TransactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka Producer — Payment Events
 * =================================
 * Publishes TransactionCreatedEvent to the 'transaction-events' topic
 * whenever a new payment order is created. This enables asynchronous
 * fraud detection via the FraudDetectionConsumer.
 */
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

    /**
     * Publishes a transaction event to Kafka for asynchronous processing.
     *
     * @param event The transaction event containing payment details
     */
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
