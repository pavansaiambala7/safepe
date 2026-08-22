package com.safepe.service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safepe.config.KafkaConfig;
import com.safepe.dto.FraudAlertEvent;
import com.safepe.dto.TransactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Kafka Consumer — Fraud Detection Pipeline
 * ============================================
 * Listens to the 'transaction-events' topic and triggers the
 * Agentic AI fraud engine for each incoming transaction.
 * Results are published to the 'fraud-alerts' topic.
 *
 * This enables fully asynchronous, event-driven fraud detection
 * that doesn't block the payment flow.
 */
@Service
@Slf4j
public class FraudDetectionConsumer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public FraudDetectionConsumer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Consumes transaction events and runs them through the AI fraud pipeline.
     * In production, this would invoke the AgenticFraudEngine for multi-step analysis.
     */
    @KafkaListener(
            topics = KafkaConfig.TRANSACTION_EVENTS_TOPIC,
            groupId = "safepe-fraud-detection-group"
    )
    public void consumeTransactionEvent(String message) {
        long startTime = System.currentTimeMillis();

        try {
            TransactionEvent event = objectMapper.readValue(message, TransactionEvent.class);
            log.info("📥 Received TransactionEvent from Kafka: txn={}, amount=₹{}, upi={}",
                    event.getTransactionId(), event.getAmount(), event.getUpiId());

            // ── Step 1: Pattern Classification ──────────────────────────
            String patternClassification = classifyTransactionPattern(event);

            // ── Step 2: Risk Score Calculation ──────────────────────────
            int riskScore = calculateRiskScore(event);
            String action = determineAction(riskScore);

            // ── Step 3: Build and publish Fraud Alert ───────────────────
            FraudAlertEvent alertEvent = FraudAlertEvent.builder()
                    .transactionId(event.getTransactionId())
                    .userId(event.getUserId())
                    .riskScore(riskScore)
                    .action(action)
                    .summary(String.format("Transaction of ₹%s to %s classified as %s risk",
                            event.getAmount(), event.getUpiId(), action))
                    .reasoningSteps(List.of(
                            "Step 1: Pattern Classification — " + patternClassification,
                            "Step 2: Merchant Trust Score — " + 
                                (event.getMerchantTrustScore() != null ? event.getMerchantTrustScore() : "Unknown"),
                            "Step 3: Risk Evaluation — Score: " + riskScore + "%, Action: " + action
                    ))
                    .matchedPatterns(List.of(patternClassification))
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .timestamp(LocalDateTime.now())
                    .build();

            // Publish fraud alert to Kafka
            String alertPayload = objectMapper.writeValueAsString(alertEvent);
            kafkaTemplate.send(KafkaConfig.FRAUD_ALERTS_TOPIC,
                    event.getTransactionId().toString(), alertPayload);

            log.info("🚨 Published FraudAlertEvent: txn={}, risk={}%, action={}",
                    event.getTransactionId(), riskScore, action);

        } catch (JsonProcessingException e) {
            log.error("❌ Failed to process TransactionEvent from Kafka: {}", e.getMessage());
        }
    }

    /**
     * Classifies the transaction pattern based on amount and merchant trust score.
     */
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

    /**
     * Calculates risk score based on merchant trust and transaction amount.
     */
    private int calculateRiskScore(TransactionEvent event) {
        int score = 10; // Base score

        // Factor 1: Merchant trust score
        if (event.getMerchantTrustScore() != null) {
            score += (int) ((1.0 - event.getMerchantTrustScore()) * 50);
        } else {
            score += 25; // Unknown merchant penalty
        }

        // Factor 2: Transaction amount
        if (event.getAmount() != null) {
            double amount = event.getAmount().doubleValue();
            if (amount > 100000) score += 30;
            else if (amount > 50000) score += 20;
            else if (amount > 10000) score += 10;
        }

        return Math.min(score, 100);
    }

    /**
     * Determines the action based on risk score thresholds.
     */
    private String determineAction(int riskScore) {
        if (riskScore >= 75) return "BLOCK";
        if (riskScore >= 45) return "FLAG_VERIFICATION";
        return "ALLOW";
    }
}
