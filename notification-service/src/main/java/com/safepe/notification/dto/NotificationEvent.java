package com.safepe.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    // Fraud-specific fields
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
