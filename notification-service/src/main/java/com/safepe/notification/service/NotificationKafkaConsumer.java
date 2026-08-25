package com.safepe.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safepe.notification.config.KafkaConfig;
import com.safepe.notification.dto.FraudAlertEvent;
import com.safepe.notification.dto.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kafka Consumer — Notification Service
 * =====================================
 * Consumes 'fraud-alerts' topic and pushes real-time Server-Sent Events (SSE)
 * notifications to connected clients.
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
            topics = KafkaConfig.FRAUD_ALERTS_TOPIC,
            groupId = "safepe-notification-group"
    )
    public void consumeFraudAlert(String message) {
        try {
            FraudAlertEvent alert = objectMapper.readValue(message, FraudAlertEvent.class);
            log.info("🔔 Received FraudAlertEvent from Kafka: txn={}, risk={}%, action={}",
                    alert.getTransactionId(), alert.getRiskScore(), alert.getAction());

            String action = alert.getAction();
            String txnId = alert.getTransactionId() != null ? alert.getTransactionId().toString() : "unknown";
            String pattern = (alert.getMatchedPatterns() != null && !alert.getMatchedPatterns().isEmpty())
                    ? alert.getMatchedPatterns().get(0)
                    : "SUSPICIOUS_PATTERN";

            if ("BLOCK".equals(action)) {
                String refundId = "rzp_rfnd_" + System.currentTimeMillis();

                // 1. High risk fraud alert
                notificationSSEService.broadcast(NotificationEvent.builder()
                        .id("notif-fraud-" + UUID.randomUUID().toString().substring(0, 8))
                        .type("FRAUD_ALERT")
                        .title("High-Risk Fraud Intercepted by SafePe AI")
                        .message(String.format("Payment was blocked by SafePe AI. Threat: %s. Risk Score: %d%%.",
                                pattern, alert.getRiskScore()))
                        .referenceId(txnId)
                        .transactionId(txnId)
                        .threatCategory(pattern)
                        .similarityMatch((double) alert.getRiskScore())
                        .matchedPatternDescription(alert.getSummary())
                        .action("BLOCK")
                        .refundId(refundId)
                        .timestamp(LocalDateTime.now())
                        .build());

                // 2. Refund initiated (1.5s delay to simulate escrow trigger)
                new Thread(() -> {
                    try {
                        Thread.sleep(1500);
                        notificationSSEService.broadcast(NotificationEvent.builder()
                                .id("notif-refund-init-" + UUID.randomUUID().toString().substring(0, 8))
                                .type("REFUND_INITIATED")
                                .title("Escrow Refund Initiated")
                                .message("Automated clawback triggered. Refund is being processed via Razorpay escrow nodal account.")
                                .referenceId(refundId)
                                .refundId(refundId)
                                .timestamp(LocalDateTime.now())
                                .build());
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }).start();

                // 3. Refund completed (3.5s delay to simulate bank processing)
                new Thread(() -> {
                    try {
                        Thread.sleep(3500);
                        notificationSSEService.broadcast(NotificationEvent.builder()
                                .id("notif-refund-done-" + UUID.randomUUID().toString().substring(0, 8))
                                .type("ESCROW_REFUND")
                                .title("SafePe Escrow Refund Completed")
                                .message("Funds have been credited back to your bank account. Escrow protection activated.")
                                .referenceId(refundId)
                                .refundId(refundId)
                                .threatCategory(pattern)
                                .similarityMatch((double) alert.getRiskScore())
                                .matchedPatternDescription("Automated clawback from nodal account before merchant settlement.")
                                .action("BLOCK")
                                .timestamp(LocalDateTime.now())
                                .build());
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }).start();

            } else if ("FLAG_VERIFICATION".equals(action)) {
                notificationSSEService.broadcast(NotificationEvent.builder()
                        .id("notif-warn-" + UUID.randomUUID().toString().substring(0, 8))
                        .type("FRAUD_ALERT")
                        .title("Suspicious Transaction Flagged for Review")
                        .message(String.format("Payment flagged for verification. Risk Score: %d%%.", alert.getRiskScore()))
                        .referenceId(txnId)
                        .action("FLAG_VERIFICATION")
                        .threatCategory(pattern)
                        .similarityMatch((double) alert.getRiskScore())
                        .matchedPatternDescription(alert.getSummary())
                        .timestamp(LocalDateTime.now())
                        .build());
            }

        } catch (JsonProcessingException e) {
            log.error("❌ Failed to process FraudAlertEvent for SSE notification: {}", e.getMessage());
        }
    }
}
