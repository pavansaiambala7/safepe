package com.safepe.fraud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckResponse {
    private String merchantName;
    private Double trustScore;
    private Boolean isVerified;
    private Boolean isFlagged;
    private Integer reportCount;
    private String riskLevel;
    private List<String> flagReasons;
    private String message;
}
