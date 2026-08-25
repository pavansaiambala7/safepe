package com.safepe.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlertEvent implements Serializable {
    private UUID transactionId;
    private String userId;
    private int riskScore;
    private String action;
    private String summary;
    private List<String> reasoningSteps;
    private List<String> matchedPatterns;
    private long processingTimeMs;
    private LocalDateTime timestamp;
}
