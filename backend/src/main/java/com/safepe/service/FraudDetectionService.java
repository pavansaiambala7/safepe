package com.safepe.service;

import com.safepe.model.Merchant;
import com.safepe.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Layer 2 — Business Logic
 * ========================
 * This service handles fraud detection for UPI payments.
 * It checks the database for known merchants and returns their trust score.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private final MerchantRepository merchantRepository;

    /**
     * Checks the trust score and risk level of a given UPI ID.
     * 
     * @param upiId The UPI ID the user is trying to pay (e.g., "reliance.retail@okaxis")
     * @return A map containing the trust score, risk level, and merchant name
     */
    public Map<String, Object> checkUpiFraudRisk(String upiId) {
        log.info("🔍 Checking fraud risk for UPI: {}", upiId);

        Map<String, Object> result = new HashMap<>();
        
        // Step 1: Look up the merchant in our database
        Optional<Merchant> merchantOpt = merchantRepository.findByUpiIdMasked(upiId);

        if (merchantOpt.isPresent()) {
            // We found the merchant!
            Merchant merchant = merchantOpt.get();
            result.put("status", "FOUND");
            result.put("merchantName", merchant.getName());
            result.put("trustScore", merchant.getTrustScore());
            result.put("isFlagged", merchant.getIsFlagged());
            
            // Add a friendly message for the frontend
            if (merchant.getIsFlagged() || merchant.getTrustScore() < 0.40) {
                result.put("warning", "🚨 HIGH RISK: This UPI ID has been reported for fraud. Do not proceed with payment.");
            } else if (merchant.getTrustScore() > 0.80) {
                result.put("message", "✅ Verified Safe Merchant");
            } else {
                result.put("message", "⚠️ Caution: Unknown or new merchant.");
            }
        } else {
            // The UPI ID is not in our database of known fraudsters — it's likely safe
            result.put("status", "NOT_FOUND");
            result.put("trustScore", 70.0); // Secure score — not in fraud database
            result.put("riskLevel", "SECURE");
            result.put("message", "✅ This UPI ID is not in our fraud database. Secure to proceed.");
        }

        return result;
    }
}
