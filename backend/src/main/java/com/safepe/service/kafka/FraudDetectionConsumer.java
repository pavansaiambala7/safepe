package com.safepe.service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safepe.config.KafkaConfig;
import com.safepe.dto.AgenticFraudResult;
import com.safepe.dto.FraudAlertEvent;
import com.safepe.dto.NotificationEvent;
import com.safepe.dto.TransactionEvent;
import com.safepe.service.NotificationSSEService;
import com.safepe.service.agent.AgenticFraudEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Kafka Consumer — Fraud Detection Pipeline
 * ============================================
 * Listens to the 'transaction-events' topic and triggers the
 * Agentic AI fraud engine for each incoming transaction.
 * Results are published to the 'fraud-alerts' topic.
 *
 * NOW ALSO: Pushes real-time notifications to the React frontend
 * via SSE (NotificationSSEService) for the bell icon.
 */
@Service
@Slf4j
public class FraudDetectionConsumer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final NotificationSSEService notificationSSEService;
    private final AgenticFraudEngine agenticFraudEngine;
    private final ObjectMapper objectMapper;

    public FraudDetectionConsumer(KafkaTemplate<String, String> kafkaTemplate,
                                  NotificationSSEService notificationSSEService,
                                  AgenticFraudEngine agenticFraudEngine) {
        this.kafkaTemplate = kafkaTemplate;
        this.notificationSSEService = notificationSSEService;
        this.agenticFraudEngine = agenticFraudEngine;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Consumes transaction events and runs them through the AI fraud pipeline.
     * Pushes fraud alerts to both Kafka and SSE (bell notifications).
     */
    @KafkaListener(
            topics = KafkaConfig.TRANSACTION_EVENTS_TOPIC,
            groupId = "safepe-fraud-detection-group"
    )
    public void consumeTransactionEvent(String message) {
        long startTime = System.currentTimeMillis();

        try {
            TransactionEvent event = objectMapper.readValue(message, TransactionEvent.class);
            log.info("Received TransactionEvent from Kafka: txn={}, amount={}, upi={}",
                    event.getTransactionId(), event.getAmount(), event.getUpiId());

            // Step 1: Pattern Classification (fast, rule-based)
            String patternClassification = classifyTransactionPattern(event);

            // Step 2: Risk Score Calculation (fast, rule-based)
            int riskScore = calculateRiskScore(event);
            String action = determineAction(riskScore);

            // Step 2b: MEDIUM-RISK ESCALATION → LLM + RAG engine
            // ------------------------------------------------------------
            // Clear ALLOW/BLOCK cases are decided cheaply by the rules above.
            // Only the uncertain, medium-risk transactions are escalated to the
            // agentic AI engine (Gemini classification → RAG vector search →
            // Gemini risk evaluation) for deeper, semantic analysis. This runs
            // here in the async Kafka consumer, so the slow LLM call never
            // blocks the user's payment.
            if ("FLAG_VERIFICATION".equals(action)) {
                try {
                    String summary = buildTransactionSummary(event);
                    AgenticFraudResult aiResult = agenticFraudEngine.analyzeWithAgents(
                            summary, event.getUpiId(), event.getUserId());
                    if (aiResult != null) {
                        log.info("Medium-risk txn {} escalated to AI engine → rule={}%, ai={}%, action={}",
                                event.getTransactionId(), riskScore, aiResult.getRiskScore(), aiResult.getAction());
                        // Trust the AI engine's deeper decision
                        riskScore = aiResult.getRiskScore();
                        action = aiResult.getAction();
                        if (aiResult.getSummary() != null && !aiResult.getSummary().isBlank()) {
                            patternClassification = "AI-Escalated: " + aiResult.getSummary();
                        }
                    }
                } catch (Exception aiEx) {
                    // Graceful degradation: keep the rule-based decision if the LLM fails
                    log.warn("AI escalation failed for txn {}, keeping rule-based decision: {}",
                            event.getTransactionId(), aiEx.getMessage());
                }
            }

            // Step 3: Build and publish Fraud Alert to Kafka
            FraudAlertEvent alertEvent = FraudAlertEvent.builder()
                    .transactionId(event.getTransactionId())
                    .userId(event.getUserId())
                    .riskScore(riskScore)
                    .action(action)
                    .summary(String.format("Transaction of %s to %s classified as %s risk",
                            event.getAmount(), event.getUpiId(), action))
                    .reasoningSteps(List.of(
                            "Step 1: Pattern Classification - " + patternClassification,
                            "Step 2: Merchant Trust Score - " +
                                (event.getMerchantTrustScore() != null ? event.getMerchantTrustScore() : "Unknown"),
                            "Step 3: Risk Evaluation - Score: " + riskScore + "%, Action: " + action
                    ))
                    .matchedPatterns(List.of(patternClassification))
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .timestamp(LocalDateTime.now())
                    .build();

            String alertPayload = objectMapper.writeValueAsString(alertEvent);
            kafkaTemplate.send(KafkaConfig.FRAUD_ALERTS_TOPIC,
                    event.getTransactionId().toString(), alertPayload);

            log.info("Published FraudAlertEvent: txn={}, risk={}%, action={}",
                    event.getTransactionId(), riskScore, action);

            // ── Push to SSE Bell Notifications ────────────────────────────
            pushFraudNotificationToSSE(event, alertEvent, riskScore, action, patternClassification);

        } catch (JsonProcessingException e) {
            log.error("Failed to process TransactionEvent from Kafka: {}", e.getMessage());
        }
    }

    /**
     * Pushes fraud detection results to the frontend bell via SSE.
     * For BLOCK actions: sends FRAUD_ALERT + REFUND_INITIATED + ESCROW_REFUND sequence.
     * For FLAG_VERIFICATION: sends a warning FRAUD_ALERT.
     * For ALLOW: sends a SUCCESS confirmation.
     */
    private void pushFraudNotificationToSSE(TransactionEvent txn, FraudAlertEvent alert,
                                             int riskScore, String action, String pattern) {
        try {
            if ("BLOCK".equals(action)) {
                // 1. Fraud Alert Notification
                String refundId = "rzp_rfnd_" + System.currentTimeMillis();
                notificationSSEService.broadcast(NotificationEvent.builder()
                        .id("notif-fraud-" + UUID.randomUUID().toString().substring(0, 8))
                        .type("FRAUD_ALERT")
                        .title("High-Risk Fraud Intercepted by SafePe AI")
                        .message(String.format("Payment of %s to %s was blocked. Threat: %s. Risk Score: %d%%.",
                                txn.getAmount(), txn.getUpiId(), pattern, riskScore))
                        .amount(txn.getAmount())
                        .upiId(txn.getUpiId())
                        .referenceId(txn.getTransactionId() != null ? txn.getTransactionId().toString() : null)
                        .transactionId(txn.getTransactionId() != null ? txn.getTransactionId().toString() : null)
                        .threatCategory(pattern)
                        .similarityMatch((double) riskScore)
                        .matchedPatternDescription(alert.getSummary())
                        .merchantName(txn.getMerchantName())
                        .merchantUpi(txn.getUpiId())
                        .merchantTrustScore(txn.getMerchantTrustScore() != null ? txn.getMerchantTrustScore() : 0.0)
                        .reportedCount(15)
                        .action("BLOCK")
                        .refundId(refundId)
                        .refundAmount(txn.getAmount())
                        .timestamp(LocalDateTime.now())
                        .build());

                // 2. Refund Initiated Notification (1.5s delay to simulate escrow trigger)
                new Thread(() -> {
                    try {
                        Thread.sleep(1500);
                        notificationSSEService.broadcast(NotificationEvent.builder()
                                .id("notif-refund-init-" + UUID.randomUUID().toString().substring(0, 8))
                                .type("REFUND_INITIATED")
                                .title("Escrow Refund Initiated")
                                .message(String.format("Automated clawback of %s triggered. Refund is being processed via Razorpay escrow.",
                                        txn.getAmount()))
                                .amount(txn.getAmount())
                                .upiId(txn.getUpiId())
                                .referenceId(refundId)
                                .refundId(refundId)
                                .refundAmount(txn.getAmount())
                                .timestamp(LocalDateTime.now())
                                .build());
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }).start();

                // 3. Refund Completed Notification (3.5s delay to simulate bank processing)
                new Thread(() -> {
                    try {
                        Thread.sleep(3500);
                        notificationSSEService.broadcast(NotificationEvent.builder()
                                .id("notif-refund-done-" + UUID.randomUUID().toString().substring(0, 8))
                                .type("ESCROW_REFUND")
                                .title("SafePe Escrow Refund Completed")
                                .message(String.format("%s has been credited back to your bank account. Escrow protection activated.",
                                        txn.getAmount()))
                                .amount(txn.getAmount())
                                .upiId(txn.getUpiId())
                                .referenceId(refundId)
                                .refundId(refundId)
                                .refundAmount(txn.getAmount())
                                .threatCategory(pattern)
                                .similarityMatch((double) riskScore)
                                .matchedPatternDescription("Automated clawback from nodal account before merchant settlement.")
                                .merchantName(txn.getMerchantName())
                                .merchantUpi(txn.getUpiId())
                                .merchantTrustScore(txn.getMerchantTrustScore() != null ? txn.getMerchantTrustScore() : 0.0)
                                .reportedCount(15)
                                .action("BLOCK")
                                .timestamp(LocalDateTime.now())
                                .build());
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }).start();

            } else if ("FLAG_VERIFICATION".equals(action)) {
                // Warning-level fraud alert
                notificationSSEService.broadcast(NotificationEvent.builder()
                        .id("notif-warn-" + UUID.randomUUID().toString().substring(0, 8))
                        .type("FRAUD_ALERT")
                        .title("Suspicious Transaction Flagged for Review")
                        .message(String.format("Payment of %s to %s flagged for verification. Risk Score: %d%%.",
                                txn.getAmount(), txn.getUpiId(), riskScore))
                        .amount(txn.getAmount())
                        .upiId(txn.getUpiId())
                        .referenceId(txn.getTransactionId() != null ? txn.getTransactionId().toString() : null)
                        .action("FLAG_VERIFICATION")
                        .threatCategory(pattern)
                        .similarityMatch((double) riskScore)
                        .matchedPatternDescription(alert.getSummary())
                        .merchantName(txn.getMerchantName())
                        .merchantUpi(txn.getUpiId())
                        .merchantTrustScore(txn.getMerchantTrustScore())
                        .timestamp(LocalDateTime.now())
                        .build());
            }
            // ALLOW actions get SUCCESS notifications from PaymentService after verification

        } catch (Exception e) {
            log.error("Failed to push fraud notification to SSE: {}", e.getMessage());
        }
    }

    /**
     * Turns a structured payment event into a natural-language summary so the
     * AI engine can embed it (for RAG vector search) and reason over it.
     * A payment isn't text on its own, so we describe it in words first.
     */
    private String buildTransactionSummary(TransactionEvent event) {
        String trust = event.getMerchantTrustScore() != null
                ? String.format("%.0f%%", event.getMerchantTrustScore() * 100)
                : "unknown";
        return String.format(
                "Payment of %s to merchant '%s' (UPI: %s). Merchant trust score: %s. Payment type: %s.",
                event.getAmount(),
                event.getMerchantName() != null ? event.getMerchantName() : "Unknown merchant",
                event.getUpiId(),
                trust,
                event.getType() != null ? event.getType() : "UPI");
    }

    private String classifyTransactionPattern(TransactionEvent event) {
        if (event.getMerchantTrustScore() != null && event.getMerchantTrustScore() < 0.3) {
            return "HIGH_RISK_MERCHANT";
        } else if (event.getAmount() != null && event.getAmount().doubleValue() > 50000) {
            return "HIGH_VALUE_TRANSACTION";
        } else if (event.getMerchantTrustScore() != null && event.getMerchantTrustScore() < 0.6) {
            return "MEDIUM_RISK_MERCHANT";
        }
        return "STANDARD_TRANSACTION";
    }

    private int calculateRiskScore(TransactionEvent event) {
        int score = 10;
        if (event.getMerchantTrustScore() != null) {
            score += (int) ((1.0 - event.getMerchantTrustScore()) * 50);
        } else {
            score += 25;
        }
        if (event.getAmount() != null) {
            double amount = event.getAmount().doubleValue();
            if (amount > 100000) score += 30;
            else if (amount > 50000) score += 20;
            else if (amount > 10000) score += 10;
        }
        return Math.min(score, 100);
    }

    private String determineAction(int riskScore) {
        if (riskScore >= 75) return "BLOCK";
        if (riskScore >= 45) return "FLAG_VERIFICATION";
        return "ALLOW";
    }
}
