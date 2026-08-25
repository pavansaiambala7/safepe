package com.safepe.fraud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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
