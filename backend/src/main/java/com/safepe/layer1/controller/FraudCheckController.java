package com.safepe.layer1.controller;

import com.safepe.dto.FraudCheckRequest;
import com.safepe.dto.FraudCheckResponse;
import com.safepe.dto.ScamSMSRequest;
import com.safepe.dto.ScamSMSResponse;
import com.safepe.layer1.service.FraudDetectionService;
import com.safepe.layer1.service.GeminiAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<FraudCheckResponse> checkMerchant(
            @RequestBody FraudCheckRequest request) {
        FraudCheckResponse response = fraudDetectionService
            .checkMerchant(request.merchantName(), request.upiId());
        return ResponseEntity.ok(response);
    }

    /**
     * Analyze an SMS message for scam indicators.
     * Problem 3: Scam SMS detector — paste message, get instant verdict.
     */
    @PostMapping("/analyze-sms")
    public ResponseEntity<ScamSMSResponse> analyzeScamSMS(
            @RequestBody ScamSMSRequest request) {
        ScamSMSResponse response = geminiAIService.analyzeScamSMS(request.content());
        return ResponseEntity.ok(response);
    }

    /**
     * Search merchants by name — returns list of matching merchants.
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchMerchants(@RequestParam String query) {
        return ResponseEntity.ok(fraudDetectionService.checkMerchant(query, null));
    }
}
