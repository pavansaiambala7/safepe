package com.safepe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO returned after a merchant fraud-check.
 * Contains trust metrics, risk classification, and human-readable flag reasons.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckResponse {

    private String merchantName;

    /** Trust score in the range [0.0, 1.0] — higher is more trustworthy. */
    private Double trustScore;

    /** Whether the merchant has been formally verified. */
    private Boolean isVerified;

    /** Whether the merchant has been flagged by users or automated systems. */
    private Boolean isFlagged;

    /** Total number of fraud/scam reports filed against this merchant. */
    private Integer reportCount;

    /** Computed risk bucket: LOW, MEDIUM, HIGH, or CRITICAL. */
    private String riskLevel;

    /** Specific reasons the merchant was flagged (empty list if clean). */
    private List<String> flagReasons;

    /** Human-readable summary message. */
    private String message;
}
