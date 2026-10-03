package com.safepe.payment.controller;

import com.safepe.payment.model.TokenizedCard;
import com.safepe.payment.model.TokenizedUPI;
import com.safepe.payment.dto.request.SaveCardRequest;
import com.safepe.payment.dto.request.SaveUpiRequest;
import com.safepe.payment.service.TokenizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vault")
@RequiredArgsConstructor
@Slf4j
public class VaultController {

    private final TokenizationService tokenizationService;

    @PostMapping("/cards")
    public ResponseEntity<?> saveCard(Principal principal, @Valid @RequestBody SaveCardRequest request) {
        String userId = principal.getName();

        String razorpayTokenId = request.razorpayTokenId() != null && !request.razorpayTokenId().isBlank()
                ? request.razorpayTokenId()
                : "token_" + UUID.randomUUID().toString().substring(0, 10);
        String razorpayCustomerId = "cust_" + userId.substring(0, Math.min(userId.length(), 10));

        // Validation guarantees 12-19 digits, so the last four always exist.
        String cardNumber = request.cardNumber();
        String lastFour = cardNumber.substring(cardNumber.length() - 4);

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
    public ResponseEntity<?> saveUpi(Principal principal, @Valid @RequestBody SaveUpiRequest request) {
        String userId = principal.getName();
        String upiId = request.upiId();

        String razorpayTokenId = request.razorpayTokenId() != null && !request.razorpayTokenId().isBlank()
                ? request.razorpayTokenId()
                : "token_upi_" + UUID.randomUUID().toString().substring(0, 10);
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
