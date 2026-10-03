package com.safepe.payment.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.safepe.payment.exception.ExternalServiceException;
import com.safepe.payment.dto.TransactionEvent;
import com.safepe.payment.model.Transaction;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final TransactionRepository transactionRepository;
    private final PaymentEventProducer paymentEventProducer;

    // Checkout signatures (razorpay_order_id|razorpay_payment_id) are HMAC'd
    // with the API key secret. The webhook secret only signs server-to-server
    // webhook bodies; using it here failed every real payment.
    @Value("${safepe.razorpay.key-secret:dummy_secret}")
    private String keySecret;

    @Value("${safepe.razorpay.key-id:rzp_test_T5AtiMDfqh5J2N}")
    private String keyId;

    @Transactional
    public Map<String, Object> createPaymentOrder(String userId, String upiId, BigDecimal amount) {
        log.info("💸 Creating payment order of ₹{} for user {} to {}", amount, userId, upiId);

        try {
            BigDecimal amountInPaise = amount.multiply(new BigDecimal(100));

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise.longValue());
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String orderId = razorpayOrder.get("id");

            Transaction transaction = Transaction.builder()
                    .userId(userId)
                    .payeeUpi(upiId)
                    .amount(amount)
                    .type("UPI")
                    .status("PENDING")
                    .razorpayOrderId(orderId)
                    .build();

            transactionRepository.save(transaction);

            // No Kafka event yet: the user has not paid. The event is published
            // from verifyPaymentSignature once Razorpay confirms the payment.

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("amount", amount);
            response.put("currency", "INR");
            response.put("dbTransactionId", transaction.getId());
            response.put("keyId", keyId);
            return response;

        } catch (RazorpayException e) {
            log.error("❌ Failed to create Razorpay Order", e);
            throw new ExternalServiceException(
                    "The payment provider could not create this order. Please retry.", e);
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

            boolean isVerified = Utils.verifyPaymentSignature(options, keySecret);

            if (isVerified) {
                log.info("Payment Verified Successfully!");
                List<Transaction> transactions = transactionRepository.findByRazorpayOrderId(orderId);
                if (!transactions.isEmpty()) {
                    Transaction tx = transactions.get(0);
                    tx.setStatus("SUCCESS");
                    tx.setRazorpayPaymentId(paymentId);
                    transactionRepository.save(tx);
                    publishSuccessEvent(tx);
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

    private void publishSuccessEvent(Transaction tx) {
        try {
            paymentEventProducer.publishTransactionEvent(TransactionEvent.builder()
                    .transactionId(tx.getId())
                    .userId(tx.getUserId())
                    .upiId(tx.getPayeeUpi())
                    .amount(tx.getAmount())
                    .currency("INR")
                    .type(tx.getType())
                    .status("SUCCESS")
                    .razorpayOrderId(tx.getRazorpayOrderId())
                    .razorpayPaymentId(tx.getRazorpayPaymentId())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception kafkaEx) {
            log.warn("⚠️ Kafka event publishing failed (non-blocking): {}", kafkaEx.getMessage());
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
            // Previously returned a Wikipedia sample QR here, which a user could
            // mistake for a real payment code. Fail visibly instead.
            throw new ExternalServiceException(
                    "The payment provider could not generate a QR code. Please retry.", e);
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
            // No payout provider is wired up: nothing is actually transferred.
            mockResponse.put("simulated", true);
            return mockResponse.toString();
        } catch (Exception e) {
            log.error("❌ Failed to initiate bank transfer", e);
            throw new ExternalServiceException(
                    "The bank transfer provider is unavailable. Please retry.", e);
        }
    }
}
