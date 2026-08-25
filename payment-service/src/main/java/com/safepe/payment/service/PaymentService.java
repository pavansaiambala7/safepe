package com.safepe.payment.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.safepe.payment.dto.TransactionEvent;
import com.safepe.payment.model.Merchant;
import com.safepe.payment.model.Transaction;
import com.safepe.payment.repository.MerchantRepository;
import com.safepe.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final TransactionRepository transactionRepository;
    private final MerchantRepository merchantRepository;
    private final PaymentEventProducer paymentEventProducer;

    @Value("${safepe.razorpay.webhook-secret:dummy_webhook_secret}")
    private String webhookSecret;

    @Value("${safepe.razorpay.key-id:rzp_test_T5AtiMDfqh5J2N}")
    private String keyId;

    @Transactional
    public Map<String, Object> createPaymentOrder(String userId, String upiId, BigDecimal amount) {
        log.info("💸 Creating payment order of ₹{} for user {} to {}", amount, userId, upiId);

        try {
            Optional<Merchant> merchantOpt = merchantRepository.findByUpiIdMasked(upiId);
            Merchant merchant = merchantOpt.orElse(null);

            BigDecimal amountInPaise = amount.multiply(new BigDecimal(100));

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise.longValue());
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String orderId = razorpayOrder.get("id");

            Transaction transaction = Transaction.builder()
                    .userId(userId)
                    .merchant(merchant)
                    .amount(amount)
                    .type("UPI")
                    .status("PENDING")
                    .razorpayOrderId(orderId)
                    .fraudScore(merchant != null ? merchant.getTrustScore() : 70.0)
                    .build();

            transactionRepository.save(transaction);

            // ── Publish to Kafka for asynchronous fraud detection ────────────
            try {
                TransactionEvent event = TransactionEvent.builder()
                        .transactionId(transaction.getId())
                        .userId(userId)
                        .upiId(upiId)
                        .amount(amount)
                        .currency("INR")
                        .type("UPI")
                        .razorpayOrderId(orderId)
                        .merchantName(merchant != null ? merchant.getName() : "Unknown")
                        .merchantTrustScore(merchant != null ? merchant.getTrustScore() : null)
                        .timestamp(LocalDateTime.now())
                        .build();
                paymentEventProducer.publishTransactionEvent(event);
            } catch (Exception kafkaEx) {
                log.warn("⚠️ Kafka event publishing failed (non-blocking): {}", kafkaEx.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("amount", amount);
            response.put("currency", "INR");
            response.put("dbTransactionId", transaction.getId());
            response.put("keyId", keyId);
            return response;

        } catch (RazorpayException e) {
            log.error("❌ Failed to create Razorpay Order", e);
            throw new RuntimeException("Payment service is currently unavailable");
        }
    }

    @Transactional
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        log.info("🔐 Verifying payment signature for Order ID: {}", orderId);

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            boolean isVerified = Utils.verifyPaymentSignature(options, webhookSecret);

            if (isVerified) {
                log.info("Payment Verified Successfully!");
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

    @Transactional
    public String generateQrCode(BigDecimal amount, String description) {
        log.info("Generating QR Code for amount: ₹{}", amount);
        try {
            JSONObject qrRequest = new JSONObject();
            qrRequest.put("type", "upi_qr");
            qrRequest.put("name", "SafePe Dynamic QR");
            qrRequest.put("usage", "single_use");
            qrRequest.put("fixed_amount", true);
            qrRequest.put("payment_amount", amount.multiply(new BigDecimal("100")).longValue());
            qrRequest.put("description", description != null ? description : "Scan to pay SafePe");

            com.razorpay.QrCode qr = razorpayClient.qrCode.create(qrRequest);
            return qr.toString();
        } catch (RazorpayException e) {
            log.error("❌ Failed to generate QR Code", e);
            return "{\"id\":\"qr_" + System.currentTimeMillis() + "\",\"image_url\":\"https://upload.wikimedia.org/wikipedia/commons/d/d0/QR_code_for_mobile_English_Wikipedia.svg\"}";
        }
    }

    @Transactional
    public String initiateBankTransfer(BigDecimal amount, String beneficiaryName, String accountNumber, String ifscCode, String purpose) {
        log.info("Initiating Bank Transfer of ₹{} to {}", amount, beneficiaryName);
        try {
            JSONObject mockResponse = new JSONObject();
            mockResponse.put("id", "pout_" + System.currentTimeMillis());
            mockResponse.put("status", "processing");
            mockResponse.put("amount", amount.multiply(new BigDecimal("100")).longValue());
            mockResponse.put("beneficiary_name", beneficiaryName);
            mockResponse.put("account_number", accountNumber);
            return mockResponse.toString();
        } catch (Exception e) {
            log.error("❌ Failed to initiate bank transfer", e);
            throw new RuntimeException("Bank Transfer service is currently unavailable");
        }
    }
}
