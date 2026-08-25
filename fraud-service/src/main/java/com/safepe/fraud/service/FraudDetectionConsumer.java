package com.safepe.fraud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safepe.fraud.config.KafkaConfig;
import com.safepe.fraud.dto.AgenticFraudResult;
import com.safepe.fraud.dto.FraudAlertEvent;
import com.safepe.fraud.dto.TransactionEvent;
import com.safepe.fraud.service.agent.AgenticFraudEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class FraudDetectionConsumer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AgenticFraudEngine agenticFraudEngine;
    private final ObjectMapper objectMapper;

    public FraudDetectionConsumer(KafkaTemplate<String, String> kafkaTemplate,
                                  AgenticFraudEngine agenticFraudEngine) {
        this.kafkaTemplate = kafkaTemplate;
        this.agenticFraudEngine = agenticFraudEngine;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

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

            String patternClassification = classifyTransactionPattern(event);
            int riskScore = calculateRiskScore(event);
            String action = determineAction(riskScore);

            if ("FLAG_VERIFICATION".equals(action)) {
                try {
                    String summary = buildTransactionSummary(event, riskScore, patternClassification);
                    AgenticFraudResult aiResult = agenticFraudEngine.analyzeWithAgents(
                            summary, event.getUpiId(), event.getUserId());
                    if (aiResult != null) {
                        log.info("Medium-risk txn {} escalated to AI engine → rule={}%, ai={}%, action={}",
                                event.getTransactionId(), riskScore, aiResult.getRiskScore(), aiResult.getAction());
                        riskScore = aiResult.getRiskScore();
                        action = aiResult.getAction();
                        if (aiResult.getSummary() != null && !aiResult.getSummary().isBlank()) {
                            patternClassification = "AI-Escalated: " + aiResult.getSummary();
                        }
                    }
                } catch (Exception aiEx) {
                    log.warn("AI escalation failed for txn {}, keeping rule-based decision: {}",
                            event.getTransactionId(), aiEx.getMessage());
                }
            }

            FraudAlertEvent alertEvent = FraudAlertEvent.builder()
                    .transactionId(event.getTransactionId())
                    .userId(event.getUserId())
                    .riskScore(riskScore)
                    .action(action)
                    .summary(String.format("Transaction of %s to %s classified as %s risk (%s)",
                            event.getAmount(), event.getUpiId(), action, patternClassification))
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

            log.info("📢 Published FraudAlertEvent to Kafka (fraud-alerts): txn={}, risk={}%, action={}",
                    event.getTransactionId(), riskScore, action);

        } catch (JsonProcessingException e) {
            log.error("Failed to process TransactionEvent from Kafka: {}", e.getMessage());
        }
    }

    private String buildTransactionSummary(TransactionEvent event, int ruleScore, String rulePattern) {
        boolean verified = event.getMerchantTrustScore() != null;
        String trust = verified
                ? String.format("%.0f%% (verified merchant)", event.getMerchantTrustScore() * 100)
                : "UNKNOWN — merchant is NOT in the verified directory (treat as elevated risk)";
        return String.format(
                "Evaluate this UPI payment for fraud risk based on the signals below.\n" +
                "- Amount: %s\n" +
                "- Merchant: %s (UPI: %s)\n" +
                "- Merchant trust score: %s\n" +
                "- Rule-based pattern: %s\n" +
                "- Rule-based preliminary risk: %d/100\n" +
                "Weigh these signals, decide a final risk level, and give a short plain-English reason.",
                event.getAmount(),
                event.getMerchantName() != null ? event.getMerchantName() : "Unknown merchant",
                event.getUpiId(),
                trust,
                rulePattern,
                ruleScore);
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
            score += 40;
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
