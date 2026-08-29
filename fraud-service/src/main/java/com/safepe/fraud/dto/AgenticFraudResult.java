package com.safepe.fraud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgenticFraudResult implements Serializable {

    private int riskScore;              // 0-100 (Risk metric)
    private int trustScore;             // 0-100 (Trust metric: >= 75 is high trust, <= 25 is low trust)
    private String trustLevel;          // HIGH_TRUST, MODERATE_TRUST, LOW_TRUST
    private boolean isRagPatternMatched;// True if vector DB matched a known scam pattern
    private double maxSimilarityPercent;// Highest cosine similarity % found in Vector DB
    private String action;              // ALLOW, BLOCK, FLAG_VERIFICATION
    private String summary;             // One-line AI verdict
    private List<ReasoningStep> reasoningSteps;
    private List<MatchedPattern> matchedPatterns;
    private long processingTimeMs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReasoningStep implements Serializable {
        private int stepNumber;
        private String stepName;
        private String description;
        private String result;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchedPattern implements Serializable {
        private String patternId;
        private String description;
        private String category;
        private String severity;
        private double similarityPercent;
    }
}
