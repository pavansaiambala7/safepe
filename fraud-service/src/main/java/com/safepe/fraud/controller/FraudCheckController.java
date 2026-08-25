package com.safepe.fraud.controller;

import com.safepe.fraud.dto.AgenticFraudResult;
import com.safepe.fraud.dto.FraudCheckRequest;
import com.safepe.fraud.dto.ScamSMSRequest;
import com.safepe.fraud.service.FraudDetectionService;
import com.safepe.fraud.service.GeminiAIService;
import com.safepe.fraud.service.agent.AgenticFraudEngine;
import com.safepe.fraud.service.rag.VectorSearchService;
import com.safepe.fraud.service.rag.VectorSearchService.VectorSearchResult;
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

    @PostMapping("/check")
    public ResponseEntity<?> checkMerchant(@RequestBody FraudCheckRequest request) {
        Map<String, Object> response = fraudDetectionService.checkUpiFraudRisk(request.upiId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/analyze-sms")
    public ResponseEntity<?> analyzeScamSMS(@RequestBody ScamSMSRequest request) {
        String aiAnalysis = geminiAIService.analyzeMessageForFraud(request.content());
        return ResponseEntity.ok(Map.of("analysis", aiAnalysis));
    }

    @PostMapping("/fd-rates")
    public ResponseEntity<?> analyzeFDRates(@RequestBody ScamSMSRequest request) {
        String aiAnalysis = geminiAIService.analyzeFDRates(request.content());
        return ResponseEntity.ok(Map.of("analysis", aiAnalysis));
    }

    @PostMapping("/agentic-analyze")
    public ResponseEntity<AgenticFraudResult> agenticAnalyze(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "");
        String upiId = request.getOrDefault("upiId", null);
        String userId = request.getOrDefault("userId", null);

        if (message.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        AgenticFraudResult result = agenticFraudEngine.analyzeWithAgents(message, upiId, userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/patterns/search")
    public ResponseEntity<List<VectorSearchResult>> searchFraudPatterns(@RequestParam String query) {
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        List<VectorSearchResult> results = vectorSearchService.searchSimilarPatterns(query);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchMerchants(@RequestParam String query) {
        return ResponseEntity.ok(fraudDetectionService.checkUpiFraudRisk(query));
    }
}
