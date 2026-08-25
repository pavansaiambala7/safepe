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

        // STEP 3: Risk Evaluation & Action Decision
        int riskScore;
        String action;
        String finalSummary;

        try {
            String evaluationPrompt = String.format(
                "You are a risk evaluation agent for SafePe bank. Based on the following evidence, " +
                "provide a risk score (0-100) and action (ALLOW, BLOCK, or FLAG_VERIFICATION).\n\n" +
                "EVIDENCE:\n" +
                "1. Classification: %s\n" +
                "2. RAG Pattern Matches: %s\n" +
                "3. Merchant Check: %s\n" +
                "4. Velocity Check: %s\n\n" +
                "Original Message: \"%s\"\n\n" +
                "Respond in EXACTLY this format:\n" +
                "RISK_SCORE: <number 0-100>\n" +
                "ACTION: <ALLOW|BLOCK|FLAG_VERIFICATION>\n" +
                "SUMMARY: <one line explanation>",
                classificationResult, ragResult, merchantResult, velocityResult, message
            );

            String aiResponse = geminiChatModel.generate(evaluationPrompt);
            riskScore = parseRiskScore(aiResponse);
            action = parseAction(aiResponse);
            finalSummary = parseSummary(aiResponse);

        } catch (Exception e) {
            riskScore = calculateFallbackRiskScore(matchedPatterns, classificationResult);
            action = riskScore >= 75 ? "BLOCK" : (riskScore >= 45 ? "FLAG_VERIFICATION" : "ALLOW");
            finalSummary = "Risk evaluated from pattern matching (AI fallback): " + classificationResult;
        }

        steps.add(ReasoningStep.builder()
                .stepNumber(3)
                .stepName("Risk Evaluation & Action Decision")
                .description("Gemini AI synthesizes all evidence to determine final risk score and action")
                .result(String.format("Risk Score: %d%% | Action: %s | %s", riskScore, action, finalSummary))
                .build());

        long processingTime = System.currentTimeMillis() - startTime;
        log.info("✅ Step 3 Complete — Risk: {}%, Action: {}, Time: {}ms", riskScore, action, processingTime);

        return AgenticFraudResult.builder()
                .riskScore(riskScore)
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
