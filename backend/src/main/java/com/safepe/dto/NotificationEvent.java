package com.safepe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SSE Notification Event DTO
 * ===========================
 * Pushed to the frontend via Server-Sent Events (SSE) whenever
 * a notable event occurs in the Kafka-driven payment pipeline.
 *
 * Types:
 *   - SUCCESS           → Payment completed successfully
 *   - FRAUD_ALERT       → AI / Kafka flagged a fraudulent transaction
 *   - ESCROW_REFUND     → Escrow refund completed & credited back
 *   - REFUND_INITIATED  → Escrow clawback has been triggered (pending)
 *   - SECURITY          → Vault / tokenization security event
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {

    private String id;
    private String type;            // SUCCESS, FRAUD_ALERT, ESCROW_REFUND, REFUND_INITIATED, SECURITY
    private String title;
    private String message;
    private BigDecimal amount;
    private String upiId;
    private String referenceId;
    private String transactionId;
    private LocalDateTime timestamp;

    // Fraud-specific fields (populated for FRAUD_ALERT / ESCROW_REFUND / REFUND_INITIATED)
    private String threatCategory;
    private Double similarityMatch;
    private String matchedPatternDescription;
    private String merchantName;
    private String merchantUpi;
    private Double merchantTrustScore;
    private Integer reportedCount;
    private String action;          // BLOCK, FLAG_VERIFICATION, ALLOW
    private String refundId;
    private BigDecimal refundAmount;
}
