package com.safepe.payment.service;

import com.safepe.payment.model.TokenizedAccount;
import com.safepe.payment.model.TokenizedCard;
import com.safepe.payment.model.TokenizedUPI;
import com.safepe.payment.repository.AccountTokenRepository;
import com.safepe.payment.repository.CardTokenRepository;
import com.safepe.payment.repository.UPITokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
