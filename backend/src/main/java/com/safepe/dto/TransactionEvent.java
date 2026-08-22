package com.safepe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kafka Event DTO — Transaction Created Event
 * =============================================
 * Published to the 'transaction-events' topic when a payment order
 * is created via Razorpay. Consumed by the Fraud Detection Consumer
 * for asynchronous AI analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent implements Serializable {

    private UUID transactionId;
    private String userId;
    private String upiId;
    private BigDecimal amount;
    private String currency;
    private String type;
    private String razorpayOrderId;
    private String merchantName;
    private Double merchantTrustScore;
    private LocalDateTime timestamp;
}
