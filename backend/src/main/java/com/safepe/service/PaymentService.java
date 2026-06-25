package com.safepe.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.safepe.model.Merchant;
import com.safepe.model.Transaction;
import com.safepe.repository.MerchantRepository;
import com.safepe.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Layer 2 — Business Logic (Payments)
 * ===================================
 * Handles the creation of payment orders via Razorpay and
 * verifies the payment signatures after successful payment.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final TransactionRepository transactionRepository;
    private final MerchantRepository merchantRepository;

    @Value("${safepe.razorpay.webhook-secret}")
    private String webhookSecret;

    /**
     * 1. Creates a Razorpay Order
     * 2. Saves a PENDING transaction in our database
     */
    @Transactional
    public Map<String, Object> createPaymentOrder(String userId, String upiId, BigDecimal amount) {
        log.info("💸 Creating payment order of ₹{} for user {} to {}", amount, userId, upiId);

        try {
            // Find the merchant receiving the money
            Optional<Merchant> merchantOpt = merchantRepository.findByUpiIdMasked(upiId);
            Merchant merchant = merchantOpt.orElse(null);

            // Razorpay expects amount in "paise" (multiply by 100)
            BigDecimal amountInPaise = amount.multiply(new BigDecimal(100));

            // Create the order request for Razorpay
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise.longValue());
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

            // Call Razorpay API
            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String orderId = razorpayOrder.get("id");

            // Save the PENDING transaction in our DB
            Transaction transaction = Transaction.builder()
                    .userId(userId)
                    .merchant(merchant)
                    .amount(amount)
                    .type("UPI")
                    .status("PENDING")
                    .razorpayOrderId(orderId)
                    .fraudScore(merchant != null ? merchant.getTrustScore() : 50.0)
                    .build();

            transactionRepository.save(transaction);

            // Return the necessary details to the React frontend
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("amount", amount);
            response.put("currency", "INR");
            response.put("dbTransactionId", transaction.getId());
            return response;

        } catch (RazorpayException e) {
            log.error("❌ Failed to create Razorpay Order", e);
            throw new RuntimeException("Payment service is currently unavailable");
        }
    }

    /**
     * Verifies the cryptographic signature sent back by Razorpay
     * to ensure the user actually paid and didn't tamper with the response.
     */
    @Transactional
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        log.info("🔐 Verifying payment signature for Order ID: {}", orderId);

        try {
            // Razorpay uses HMAC SHA256 to hash the orderId + paymentId with our secret key.
            // If the signature matches, the payment is 100% authentic.
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            boolean isVerified = Utils.verifyPaymentSignature(options, webhookSecret);

            if (isVerified) {
                log.info("✅ Payment Verified Successfully!");
                // Update our database to SUCCESS
                List<Transaction> transactions = transactionRepository.findByRazorpayOrderId(orderId);
                if (!transactions.isEmpty()) {
                    Transaction tx = transactions.get(0);
                    tx.setStatus("SUCCESS");
                    tx.setRazorpayPaymentId(paymentId);
                    transactionRepository.save(tx);
                }
                return true;
            } else {
                log.warn("🚨 PAYMENT VERIFICATION FAILED. Possible spoofing attack.");
                return false;
            }
        } catch (RazorpayException e) {
            log.error("❌ Error verifying signature", e);
            return false;
        }
    }
}
