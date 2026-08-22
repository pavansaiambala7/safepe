package com.safepe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Agentic Fraud Analysis Result
 * ===============================
 * Structured output from the multi-step Agentic AI Fraud Engine.
 * Contains the complete reasoning chain from all 3 analysis steps:
 *   Step 1: Pattern Classification
 *   Step 2: RAG Vector Search (semantic fraud matching)
 *   Step 3: Risk Evaluation & Action Decision
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgenticFraudResult implements Serializable {

    private int riskScore;              // 0-100
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
