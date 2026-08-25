package com.safepe.fraud.config;

import com.safepe.fraud.model.FraudPattern;
import com.safepe.fraud.repository.FraudPatternRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class FraudPatternSeeder implements CommandLineRunner {

    private final FraudPatternRepository fraudPatternRepository;

    @Override
    public void run(String... args) {
        if (fraudPatternRepository.count() > 0) {
            log.info("📋 Fraud patterns already seeded ({} records)", fraudPatternRepository.count());
            return;
        }

        log.info("🌱 Seeding fraud pattern knowledge base...");

        List<FraudPattern> patterns = List.of(
            FraudPattern.builder()
                .patternDescription("Dear customer your SBI account has been blocked. Click this link to verify your KYC immediately or your account will be permanently closed.")
                .category("PHISHING")
                .severity("CRITICAL")
                .keywords("account blocked, click link, verify KYC, permanently closed")
                .build(),

            FraudPattern.builder()
                .patternDescription("Your bank account will be suspended within 24 hours. Update your PAN card details by clicking the link below to avoid disruption.")
                .category("PHISHING")
                .severity("CRITICAL")
                .keywords("suspended, PAN card, update details, click link")
                .build(),

            FraudPattern.builder()
                .patternDescription("URGENT: Your credit card has been compromised. Call this number immediately to block your card and secure your funds.")
                .category("PHISHING")
                .severity("HIGH")
                .keywords("credit card compromised, call number, block card, urgent")
                .build(),

            FraudPattern.builder()
                .patternDescription("You have received a payment of Rs 50000. To accept, enter your UPI PIN on the collect request sent to your phone.")
                .category("UPI_FRAUD")
                .severity("CRITICAL")
                .keywords("received payment, enter UPI PIN, collect request")
                .build(),

            FraudPattern.builder()
                .patternDescription("Congratulations! You won a cashback of Rs 10000. Scan this QR code to claim your reward directly to your bank account.")
                .category("UPI_FRAUD")
                .severity("HIGH")
                .keywords("cashback, scan QR code, claim reward, won")
                .build(),

            FraudPattern.builder()
                .patternDescription("I accidentally sent you money through UPI. Please return it by scanning this QR code or sending to this UPI ID.")
                .category("UPI_FRAUD")
                .severity("HIGH")
                .keywords("accidentally sent, return money, scan QR, UPI ID")
                .build(),

            FraudPattern.builder()
                .patternDescription("Instant loan approved! Get Rs 500000 at 0% interest. Just pay Rs 5000 processing fee to activate your loan within 1 hour.")
                .category("LOAN_SCAM")
                .severity("HIGH")
                .keywords("instant loan, 0% interest, processing fee, approved")
                .build(),

            FraudPattern.builder()
                .patternDescription("Your pre-approved personal loan of Rs 200000 is ready. Pay a small insurance premium of Rs 3000 to receive the funds today.")
                .category("LOAN_SCAM")
                .severity("HIGH")
                .keywords("pre-approved loan, insurance premium, pay to receive")
                .build(),

            FraudPattern.builder()
                .patternDescription("Your Paytm KYC has expired. Complete your KYC verification now by sharing your Aadhaar number and OTP or your wallet will be blocked.")
                .category("KYC_FRAUD")
                .severity("CRITICAL")
                .keywords("KYC expired, share Aadhaar, OTP, wallet blocked")
                .build(),

            FraudPattern.builder()
                .patternDescription("RBI mandate: All bank accounts must complete video KYC by this month. Download this app and complete verification to avoid account freeze.")
                .category("KYC_FRAUD")
                .severity("HIGH")
                .keywords("RBI mandate, video KYC, download app, account freeze")
                .build(),

            FraudPattern.builder()
                .patternDescription("Congratulations! Your mobile number has won Rs 25 Lakhs in the Jio KBC lottery. Pay Rs 10000 tax to claim your prize money.")
                .category("LOTTERY_SCAM")
                .severity("HIGH")
                .keywords("lottery, won prize, pay tax, claim money, KBC")
                .build(),

            FraudPattern.builder()
                .patternDescription("You have been selected as the lucky winner of iPhone 15 Pro. Share your bank details and pay shipping charges of Rs 2000.")
                .category("LOTTERY_SCAM")
                .severity("MEDIUM")
                .keywords("lucky winner, share bank details, shipping charges, iPhone")
                .build(),

            FraudPattern.builder()
                .patternDescription("Join our WhatsApp trading group for guaranteed 500% returns in crypto. Minimum investment Rs 50000. Limited spots available.")
                .category("INVESTMENT_FRAUD")
                .severity("CRITICAL")
                .keywords("WhatsApp group, guaranteed returns, crypto, minimum investment")
                .build(),

            FraudPattern.builder()
                .patternDescription("Double your money in 30 days! Invest in our IPO-backed scheme. Government approved. Risk-free returns guaranteed.")
                .category("INVESTMENT_FRAUD")
                .severity("HIGH")
                .keywords("double money, IPO, government approved, risk-free, guaranteed")
                .build(),

            FraudPattern.builder()
                .patternDescription("Earn Rs 50000 daily from home with just 2 hours of work. Join our stock market advisory service. No experience needed.")
                .category("INVESTMENT_FRAUD")
                .severity("MEDIUM")
                .keywords("earn daily, work from home, stock market, no experience")
                .build(),

            FraudPattern.builder()
                .patternDescription("This is the police cyber crime department calling. Your Aadhaar has been used for illegal activities. Transfer Rs 100000 to this account to clear your name.")
                .category("IMPERSONATION")
                .severity("CRITICAL")
                .keywords("police, cyber crime, Aadhaar, illegal, transfer money")
                .build(),

            FraudPattern.builder()
                .patternDescription("I am calling from Amazon customer care. Your recent order has a refund of Rs 15000. Please share your bank details and OTP to process the refund.")
                .category("IMPERSONATION")
                .severity("HIGH")
                .keywords("Amazon, customer care, refund, share bank details, OTP")
                .build(),

            FraudPattern.builder()
                .patternDescription("This is your bank manager speaking. We need to verify your account. Please share your debit card number, CVV, and expiry date for security update.")
                .category("IMPERSONATION")
                .severity("CRITICAL")
                .keywords("bank manager, verify account, debit card, CVV, expiry")
                .build(),

            FraudPattern.builder()
                .patternDescription("We detected suspicious activity on your account. To prevent unauthorized transactions, please install this remote access app TeamViewer and let us help secure your account.")
                .category("VISHING")
                .severity("CRITICAL")
                .keywords("suspicious activity, remote access, TeamViewer, install app")
                .build(),

            FraudPattern.builder()
                .patternDescription("Your electricity connection will be disconnected in 2 hours due to pending bill. Pay immediately by calling this number or clicking this link.")
                .category("VISHING")
                .severity("MEDIUM")
                .keywords("electricity disconnected, pending bill, pay immediately, call number")
                .build(),

            FraudPattern.builder()
                .patternDescription("IRCTC refund of Rs 5000 is pending. Your train ticket was cancelled. Click this link and enter your bank details to receive the refund.")
                .category("VISHING")
                .severity("HIGH")
                .keywords("IRCTC, refund pending, cancelled ticket, bank details, click link")
                .build()
        );

        fraudPatternRepository.saveAll(patterns);
        log.info("✅ Seeded {} fraud patterns across 8 categories", patterns.size());
    }
}
