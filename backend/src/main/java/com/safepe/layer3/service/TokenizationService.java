package com.safepe.layer3.service;

import com.safepe.layer3.entity.TokenizedAccount;
import com.safepe.layer3.entity.TokenizedCard;
import com.safepe.layer3.entity.TokenizedUPI;
import com.safepe.layer3.repository.AccountTokenRepository;
import com.safepe.layer3.repository.CardTokenRepository;
import com.safepe.layer3.repository.UPITokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Layer 3 — Tokenization Service
 * ==============================
 * PCI DSS Compliant Tokenization Service.
 * This service is responsible for securely interacting with the Razorpay API 
 * to store and retrieve Network Tokens instead of storing raw PANs or bank details.
 */
@Service
@Slf4j
public class TokenizationService {

    private final CardTokenRepository cardTokenRepository;
    private final UPITokenRepository upiTokenRepository;
    private final AccountTokenRepository accountTokenRepository;

    public TokenizationService(
            CardTokenRepository cardTokenRepository,
            UPITokenRepository upiTokenRepository,
            AccountTokenRepository accountTokenRepository
    ) {
        this.cardTokenRepository = cardTokenRepository;
        this.upiTokenRepository = upiTokenRepository;
        this.accountTokenRepository = accountTokenRepository;
        log.info("✅ TokenizationService initialized — PCI DSS Compliant Storage ready");
    }

    /**
     * Mocks a Razorpay Token creation for a Card.
     * In a real implementation, the frontend securely sends the card to Razorpay,
     * and passes the token_id to this backend to be saved.
     */
    public TokenizedCard saveCardToken(String userId, String razorpayCustomerId, String razorpayTokenId, String cardLastFour, String cardNetwork) {
        Optional<TokenizedCard> existing = cardTokenRepository.findByRazorpayTokenId(razorpayTokenId);
        if (existing.isPresent()) {
            return existing.get();
        }

        TokenizedCard card = TokenizedCard.builder()
                .userId(userId)
                .razorpayCustomerId(razorpayCustomerId)
                .razorpayTokenId(razorpayTokenId)
                .cardLastFour(cardLastFour)
                .cardNetwork(cardNetwork)
                .build();

        return cardTokenRepository.save(card);
    }

    /**
     * Mocks a Razorpay Token creation for a Bank Account.
     */
    public TokenizedAccount saveAccountToken(String userId, String razorpayCustomerId, String razorpayTokenId, String accountLastFour, String bankName, String ifscCode) {
        Optional<TokenizedAccount> existing = accountTokenRepository.findByRazorpayTokenId(razorpayTokenId);
        if (existing.isPresent()) {
            return existing.get();
        }

        TokenizedAccount account = TokenizedAccount.builder()
                .userId(userId)
                .razorpayCustomerId(razorpayCustomerId)
                .razorpayTokenId(razorpayTokenId)
                .accountLastFour(accountLastFour)
                .bankName(bankName)
                .ifscCode(ifscCode)
                .build();

        return accountTokenRepository.save(account);
    }

    /**
     * Mocks a Razorpay Token creation for a UPI ID.
     */
    public TokenizedUPI saveUPIToken(String userId, String razorpayCustomerId, String razorpayTokenId, String maskedUpi) {
        Optional<TokenizedUPI> existing = upiTokenRepository.findByRazorpayTokenId(razorpayTokenId);
        if (existing.isPresent()) {
            return existing.get();
        }

        TokenizedUPI upi = TokenizedUPI.builder()
                .userId(userId)
                .razorpayCustomerId(razorpayCustomerId)
                .razorpayTokenId(razorpayTokenId)
                .maskedUpi(maskedUpi)
                .build();

        return upiTokenRepository.save(upi);
    }
}
