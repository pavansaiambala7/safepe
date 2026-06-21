package com.safepe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO from the AI-powered SMS scam analyser.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScamSMSResponse {

    /** Classification result: SAFE, SUSPICIOUS, or SCAM. */
    private String classification;

    /** Model confidence in the classification (0.0–1.0). */
    private Double confidence;

    /** Specific red-flag indicators found in the SMS text. */
    private List<String> redFlags;

    /** Natural-language explanation of the classification decision. */
    private String explanation;

    /** Actionable advice for the user (e.g. "Do not click the link"). */
    private String advice;
}
