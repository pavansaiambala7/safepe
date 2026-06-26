package com.safepe.layer1.controller;

import com.safepe.dto.FraudCheckRequest;
import com.safepe.dto.ScamSMSRequest;
import com.safepe.service.FraudDetectionService;
import com.safepe.service.GeminiAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
public class FraudCheckController {

    private final FraudDetectionService fraudDetectionService;
    private final GeminiAIService geminiAIService;

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
     * Search merchants by name — returns list of matching merchants.
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchMerchants(@RequestParam String query) {
        return ResponseEntity.ok(fraudDetectionService.checkUpiFraudRisk(query));
    }
}
