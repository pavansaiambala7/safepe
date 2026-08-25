package com.safepe.fraud.service;

import com.safepe.fraud.model.Merchant;
import com.safepe.fraud.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private final MerchantRepository merchantRepository;

    public Map<String, Object> checkUpiFraudRisk(String upiId) {
        log.info("🔍 Checking fraud risk for UPI: {}", upiId);

        Map<String, Object> result = new HashMap<>();
        Optional<Merchant> merchantOpt = merchantRepository.findByUpiIdMasked(upiId);

        if (merchantOpt.isPresent()) {
            Merchant merchant = merchantOpt.get();
            result.put("status", "FOUND");
            result.put("merchantName", merchant.getName());
            result.put("trustScore", merchant.getTrustScore());
            result.put("isFlagged", merchant.getIsFlagged());

            if (merchant.getIsFlagged() || merchant.getTrustScore() < 0.40) {
                result.put("warning", "🚨 HIGH RISK: This UPI ID has been reported for fraud. Do not proceed with payment.");
            } else if (merchant.getTrustScore() > 0.80) {
                result.put("message", "✅ Verified Safe Merchant");
            } else {
                result.put("message", "⚠️ Caution: Unknown or new merchant.");
            }
        } else {
            result.put("status", "NOT_FOUND");
            result.put("trustScore", 70.0);
            result.put("riskLevel", "SECURE");
            result.put("message", "✅ This UPI ID is not in our fraud database. Secure to proceed.");
        }

        return result;
    }
}
