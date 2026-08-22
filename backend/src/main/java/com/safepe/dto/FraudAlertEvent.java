package com.safepe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Kafka Event DTO — Fraud Alert Event
 * =====================================
 * Published to the 'fraud-alerts' topic when the Agentic AI
 * fraud engine completes analysis of a transaction.
 * Actions: ALLOW, BLOCK, FLAG_VERIFICATION
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlertEvent implements Serializable {

    private UUID transactionId;
    private String userId;
    private int riskScore;           // 0-100
    private String action;           // ALLOW, BLOCK, FLAG_VERIFICATION
    private String summary;          // One-line AI summary
    private List<String> reasoningSteps;
    private List<String> matchedPatterns;
    private long processingTimeMs;
    private LocalDateTime timestamp;
}
