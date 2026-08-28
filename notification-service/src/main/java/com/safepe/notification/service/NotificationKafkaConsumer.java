package com.safepe.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safepe.notification.config.KafkaConfig;
import com.safepe.notification.dto.NotificationEvent;
import com.safepe.notification.dto.TransactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kafka Consumer — Notification Service
 * =====================================
 * Consumes 'transaction-events' topic and pushes real-time Server-Sent Events (SSE)
 * notifications to connected clients for payment activity.
 */
@Service
@Slf4j
public class NotificationKafkaConsumer {

    private final NotificationSSEService notificationSSEService;
    private final ObjectMapper objectMapper;

    public NotificationKafkaConsumer(NotificationSSEService notificationSSEService) {
        this.notificationSSEService = notificationSSEService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @KafkaListener(
            topics = KafkaConfig.TRANSACTION_EVENTS_TOPIC,
            groupId = "safepe-notification-group"
    )
    public void consumeTransactionEvent(String message) {
        try {
            TransactionEvent event = objectMapper.readValue(message, TransactionEvent.class);
            log.info("🔔 Received TransactionEvent from Kafka: txn={}, amount={}, upi={}",
                    event.getTransactionId(), event.getAmount(), event.getUpiId());

            String upiId = event.getUpiId() != null ? event.getUpiId() : "unknown";
            String amount = event.getAmount() != null ? "₹" + event.getAmount() : "₹0";
            String txnId = event.getTransactionId() != null ? event.getTransactionId().toString() : "unknown";

            // Payment success notification
            notificationSSEService.broadcast(NotificationEvent.builder()
                    .id("notif-payment-" + UUID.randomUUID().toString().substring(0, 8))
                    .type("SUCCESS")
                    .title("Payment Successful")
                    .message(String.format("Payment of %s to %s was successful.", amount, upiId))
                    .amount(event.getAmount())
                    .upiId(upiId)
                    .referenceId(txnId)
                    .transactionId(txnId)
                    .timestamp(LocalDateTime.now())
                    .build());

        } catch (JsonProcessingException e) {
            log.error("❌ Failed to process TransactionEvent for SSE notification: {}", e.getMessage());
        }
    }
}
