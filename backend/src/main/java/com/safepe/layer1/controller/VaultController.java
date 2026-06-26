package com.safepe.layer1.controller;

import com.safepe.layer3.entity.TokenizedCard;
import com.safepe.layer3.entity.TokenizedUPI;
import com.safepe.layer3.service.TokenizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * Layer 1 — Public API for the PCI DSS Compliant Token Vault
 * ==========================================================
 * Endpoints to handle Tokenization of payment instruments.
 */
@RestController
@RequestMapping("/api/v1/vault")
@RequiredArgsConstructor
@Slf4j
public class VaultController {

    private final TokenizationService tokenizationService;

    @PostMapping("/cards")
    public ResponseEntity<?> saveCard(Principal principal, @RequestBody Map<String, String> request) {
        String userId = principal != null ? principal.getName() : "user_123_temp";
        
        // In a real flow, the frontend tokenizes the card via Razorpay Checkout
        // and sends us the razorpay_token_id. Here we mock it.
        String razorpayTokenId = request.getOrDefault("razorpayTokenId", "token_" + UUID.randomUUID().toString().substring(0, 10));
        String razorpayCustomerId = "cust_" + userId.substring(0, Math.min(userId.length(), 10));
        
        String cardNumber = request.getOrDefault("cardNumber", "0000");
        String lastFour = cardNumber.length() >= 4 ? cardNumber.substring(cardNumber.length() - 4) : "0000";

        TokenizedCard savedCard = tokenizationService.saveCardToken(
                userId, 
                razorpayCustomerId, 
                razorpayTokenId, 
                lastFour, 
                "VISA"
        );
        
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Card securely tokenized via Razorpay!",
                "maskedCard", savedCard.getCardNetwork() + " ending in " + savedCard.getCardLastFour(),
                "tokenId", savedCard.getRazorpayTokenId()
        ));
    }

    @PostMapping("/upi")
    public ResponseEntity<?> saveUpi(Principal principal, @RequestBody Map<String, String> request) {
        String userId = principal != null ? principal.getName() : "user_123_temp";
        String upiId = request.get("upiId");
        
        String razorpayTokenId = request.getOrDefault("razorpayTokenId", "token_upi_" + UUID.randomUUID().toString().substring(0, 10));
        String razorpayCustomerId = "cust_" + userId.substring(0, Math.min(userId.length(), 10));
        
        String maskedUpi = maskUPI(upiId);
        
        TokenizedUPI savedUpi = tokenizationService.saveUPIToken(userId, razorpayCustomerId, razorpayTokenId, maskedUpi);
        
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS", 
                "message", "UPI ID securely tokenized via Razorpay!",
                "maskedUpi", savedUpi.getMaskedUpi(),
                "tokenId", savedUpi.getRazorpayTokenId()
        ));
    }
    
    private String maskUPI(String upiId) {
        if (upiId == null) return "";
        int atIndex = upiId.indexOf('@');
        if (atIndex <= 2) {
            return "***" + upiId.substring(Math.max(0, atIndex));
        }
        return upiId.substring(0, 2) + "***" + upiId.substring(atIndex);
    }
}
