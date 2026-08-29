package com.safepe.fraud.service.agent;

import com.safepe.fraud.dto.AgenticFraudResult;
import com.safepe.fraud.dto.AgenticFraudResult.MatchedPattern;
import com.safepe.fraud.dto.AgenticFraudResult.ReasoningStep;
import com.safepe.fraud.service.rag.VectorSearchService.VectorSearchResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgenticFraudEngine {

    private final FraudAnalysisTools fraudAnalysisTools;
    private final ChatLanguageModel geminiChatModel;

    public AgenticFraudResult analyzeWithAgents(String message, String upiId, String userId) {
        long startTime = System.currentTimeMillis();
        List<ReasoningStep> steps = new ArrayList<>();
        List<MatchedPattern> matchedPatterns = new ArrayList<>();

        log.info("🤖 [Agentic Engine] Starting 3-step fraud analysis for: '{}'",
                message.substring(0, Math.min(80, message.length())));

        // STEP 1: Pattern Classification via Gemini AI
        String classificationResult;
        try {
            String classificationPrompt =
                "You are a fraud classification agent. Classify this message into ONE category: " +
                "PHISHING, UPI_FRAUD, LOAN_SCAM, KYC_FRAUD, LOTTERY_SCAM, INVESTMENT_FRAUD, " +
                "IMPERSONATION, VISHING, or LEGITIMATE. " +
                "Respond with ONLY the category name and a one-line reason. " +
                "Message: \"" + message + "\"";
            classificationResult = geminiChatModel.generate(classificationPrompt);
        } catch (Exception e) {
            classificationResult = "UNKNOWN — Classification failed: " + e.getMessage();
        }

        steps.add(ReasoningStep.builder()
                .stepNumber(1)
                .stepName("Pattern Classification")
                .description("Gemini AI classifies the message into a fraud category")
                .result(classificationResult)
                .build());

        // STEP 2: RAG Vector Search & Context Gathering
        String ragResult;
        try {
            ragResult = fraudAnalysisTools.searchFraudPatterns(message);
            List<VectorSearchResult> rawResults = fraudAnalysisTools.getRawSearchResults(message);
            matchedPatterns = rawResults.stream()
                    .map(r -> MatchedPattern.builder()
                            .patternId(r.getPatternId())
                            .description(r.getPatternDescription())
                            .category(r.getCategory())
                            .severity(r.getSeverity())
                            .similarityPercent(r.getSimilarityScore())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            ragResult = "RAG search failed: " + e.getMessage();
        }

        String merchantResult = "No UPI ID provided for merchant verification";
        if (upiId != null && !upiId.isBlank()) {
            try {
                merchantResult = fraudAnalysisTools.checkMerchantTrustScore(upiId);
            } catch (Exception e) {
                merchantResult = "Merchant lookup failed: " + e.getMessage();
            }
        }

        String velocityResult = "No user ID provided for velocity analysis";
        if (userId != null && !userId.isBlank()) {
            try {
                velocityResult = fraudAnalysisTools.analyzeTransactionVelocity(userId);
            } catch (Exception e) {
                velocityResult = "Velocity analysis failed: " + e.getMessage();
            }
        }

        steps.add(ReasoningStep.builder()
                .stepNumber(2)
                .stepName("RAG Vector Search & Context Gathering")
                .description("Semantic search against fraud patterns via Gemini Embeddings + pgvector.")
                .result(String.format("RAG: %s\nMerchant: %s\nVelocity: %s",
                        ragResult, merchantResult, velocityResult))
                .build());

        // STEP 3: RAG Vector DB Trust & Risk Evaluation
        boolean isRagPatternMatched = !matchedPatterns.isEmpty();
        double maxSimilarity = matchedPatterns.stream()
                .mapToDouble(MatchedPattern::getSimilarityPercent)
                .max().orElse(0.0);

        int riskScore;
        int trustScore;
        String trustLevel;
        String action;
        String finalSummary;

        if (isRagPatternMatched && maxSimilarity >= 60.0) {
            // Pattern found in Vector DB: Risk is 75% or above, Trust Score is low
            riskScore = (int) Math.max(75, Math.min(99, Math.round(maxSimilarity)));
            trustScore = Math.max(1, 100 - riskScore); // Trust <= 25%
            trustLevel = "LOW_TRUST";
            action = riskScore >= 85 ? "BLOCK" : "FLAG_VERIFICATION";
            finalSummary = String.format("RAG Vector DB match (%.1f%% similarity to known fraud). High risk detected.", maxSimilarity);
        } else {
            // No matching fraud pattern in Vector DB: Risk is below 50% (Low Risk), High Trust Score
            riskScore = 15; // Low risk (< 50%)
            trustScore = 92; // High trust (> 75%)
            trustLevel = "HIGH_TRUST";
            action = "ALLOW";
            finalSummary = "RAG Vector DB verified: No matching fraud signatures detected. Transaction cleared.";
        }

        try {
            String evaluationPrompt = String.format(
                "You are a trust and risk evaluation agent for SafePe bank. Based on the evidence below, " +
                "provide a final concise summary for the user and security logs.\n\n" +
                "EVIDENCE:\n" +
                "1. Classification: %s\n" +
                "2. RAG Pattern Matches: %s (Max Similarity: %.1f%%)\n" +
                "3. Merchant Check: %s\n" +
                "4. Velocity Check: %s\n" +
                "5. Calculated Trust Score: %d%% | Risk Score: %d%% | Action: %s\n\n" +
                "Original Message: \"%s\"\n\n" +
                "Respond in ONE concise line explaining the verdict.",
                classificationResult, ragResult, maxSimilarity, merchantResult, velocityResult,
                trustScore, riskScore, action, message
            );

            String aiSummary = geminiChatModel.generate(evaluationPrompt);
            if (aiSummary != null && !aiSummary.isBlank()) {
                finalSummary = aiSummary.trim().replace("\n", " ");
            }
        } catch (Exception e) {
            log.warn("Gemini summary synthesis skipped: {}", e.getMessage());
        }

        steps.add(ReasoningStep.builder()
                .stepNumber(3)
                .stepName("Trust & Risk Evaluation (RAG Vector Grounded)")
                .description("Synthesizes Vector DB pattern cosine similarity to compute final Trust Score and Action")
                .result(String.format("Trust Score: %d%% (Level: %s) | Risk: %d%% | Action: %s | %s",
                        trustScore, trustLevel, riskScore, action, finalSummary))
                .build());

        long processingTime = System.currentTimeMillis() - startTime;
        log.info("✅ Step 3 Complete — Trust: {}%, Risk: {}%, Action: {}, Time: {}ms",
                trustScore, riskScore, action, processingTime);

        return AgenticFraudResult.builder()
                .riskScore(riskScore)
                .trustScore(trustScore)
                .trustLevel(trustLevel)
                .isRagPatternMatched(isRagPatternMatched)
                .maxSimilarityPercent(Math.round(maxSimilarity * 100.0) / 100.0)
                .action(action)
                .summary(finalSummary)
                .reasoningSteps(steps)
                .matchedPatterns(matchedPatterns)
                .processingTimeMs(processingTime)
                .build();
    }

    private int parseRiskScore(String response) {
        try {
            for (String line : response.split("\n")) {
                if (line.toUpperCase().contains("RISK_SCORE")) {
                    String num = line.replaceAll("[^0-9]", "");
                    if (!num.isEmpty()) {
                        return Math.min(100, Math.max(0, Integer.parseInt(num)));
                    }
                }
            }
        } catch (Exception ignored) {}
        return 50;
    }

    private String parseAction(String response) {
        String upper = response.toUpperCase();
        if (upper.contains("BLOCK")) return "BLOCK";
        if (upper.contains("FLAG_VERIFICATION") || upper.contains("FLAG")) return "FLAG_VERIFICATION";
        return "ALLOW";
    }

    private String parseSummary(String response) {
        try {
            for (String line : response.split("\n")) {
                if (line.toUpperCase().contains("SUMMARY")) {
                    return line.replaceFirst("(?i)SUMMARY\\s*:?\\s*", "").trim();
                }
            }
        } catch (Exception ignored) {}
        return response.substring(0, Math.min(200, response.length()));
    }

    private int calculateFallbackRiskScore(List<MatchedPattern> patterns, String classification) {
        int score = 20;

        if (!patterns.isEmpty()) {
            double maxSimilarity = patterns.stream()
                    .mapToDouble(MatchedPattern::getSimilarityPercent)
                    .max().orElse(0);
            score += (int) (maxSimilarity * 0.6);
        }

        String upper = classification.toUpperCase();
        if (upper.contains("CRITICAL") || upper.contains("PHISHING") || upper.contains("IMPERSONATION")) {
            score += 25;
        } else if (upper.contains("HIGH") || upper.contains("UPI_FRAUD")) {
            score += 15;
        }

        return Math.min(100, score);
    }
}
