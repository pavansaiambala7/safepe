package com.safepe.layer1.controller;

import com.safepe.dto.AgenticFraudResult;
import com.safepe.dto.FraudCheckRequest;
import com.safepe.dto.ScamSMSRequest;
import com.safepe.service.FraudDetectionService;
import com.safepe.service.GeminiAIService;
import com.safepe.service.agent.AgenticFraudEngine;
import com.safepe.service.rag.VectorSearchService;
import com.safepe.service.rag.VectorSearchService.VectorSearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
public class FraudCheckController {

    private final FraudDetectionService fraudDetectionService;
    private final GeminiAIService geminiAIService;
    private final AgenticFraudEngine agenticFraudEngine;
    private final VectorSearchService vectorSearchService;

    /**
     * Check a merchant or UPI ID for fraud indicators.
     * Problem 3: Fraud warnings BEFORE payment, not after.
     */
    @PostMapping("/check")
    public ResponseEntity<?> checkMerchant(
            @RequestBody FraudCheckRequest request) {
        Map<String, Object> response = fraudDetectionService.checkUpiFraudRisk(request.upiId());
        return ResponseEntity.ok(response);
    }

    /**
     * Analyze an SMS message for scam indicators.
     * Problem 3: Scam SMS detector — paste message, get instant verdict.
     */
    @PostMapping("/analyze-sms")
    public ResponseEntity<?> analyzeScamSMS(
            @RequestBody ScamSMSRequest request) {
        String aiAnalysis = geminiAIService.analyzeMessageForFraud(request.content());
        return ResponseEntity.ok(Map.of("analysis", aiAnalysis));
    }

    @PostMapping("/fd-rates")
    public ResponseEntity<?> analyzeFDRates(
            @RequestBody ScamSMSRequest request) { // Reusing ScamSMSRequest for the 'content' field
        String aiAnalysis = geminiAIService.analyzeFDRates(request.content());
        return ResponseEntity.ok(Map.of("analysis", aiAnalysis));
    }

    /**
     * 🤖 Agentic AI Fraud Analysis — Multi-Step Reasoning Engine
     * ============================================================
     * Runs the full 3-step agentic pipeline:
     *   Step 1: Pattern Classification (Gemini AI)
     *   Step 2: RAG Vector Search (Gemini Embeddings + pgvector)
     *   Step 3: Risk Evaluation & Action Decision (Gemini AI)
     *
     * Request body: { "message": "...", "upiId": "...", "userId": "..." }
     */
    @PostMapping("/agentic-analyze")
    public ResponseEntity<AgenticFraudResult> agenticAnalyze(
            @RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "");
        String upiId = request.getOrDefault("upiId", null);
        String userId = request.getOrDefault("userId", null);

        if (message.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        AgenticFraudResult result = agenticFraudEngine.analyzeWithAgents(message, upiId, userId);
        return ResponseEntity.ok(result);
    }

    /**
     * 🔍 RAG Semantic Search — Find similar fraud patterns
     * ======================================================
     * Searches the fraud pattern vector store for semantically
     * similar patterns using Gemini Embeddings + pgvector.
     *
     * Query param: ?query=suspicious+message+text
     */
    @GetMapping("/patterns/search")
    public ResponseEntity<List<VectorSearchResult>> searchFraudPatterns(
            @RequestParam String query) {
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        List<VectorSearchResult> results = vectorSearchService.searchSimilarPatterns(query);
        return ResponseEntity.ok(results);
    }

    /**
     * Search merchants by name — returns list of matching merchants.
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchMerchants(@RequestParam String query) {
        return ResponseEntity.ok(fraudDetectionService.checkUpiFraudRisk(query));
    }
}
