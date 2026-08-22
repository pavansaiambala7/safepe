package com.safepe.layer1.controller;

import com.safepe.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Layer 1 — Public API for Payments
 * =================================
 * The React frontend calls these endpoints to start a payment
 * and to verify a payment after it's done.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Step 1: Frontend says "User wants to pay ₹500 to reliance@oksbi"
     */
    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> requestData) {
        // In a real app, we extract userId from the Clerk JWT Token securely!
        String userId = "user_123_temp"; 
        
        String upiId = (String) requestData.get("upiId");
        
        // Convert the string amount to a BigDecimal safely
        String amountStr = requestData.get("amount").toString();
        BigDecimal amount = new BigDecimal(amountStr);

        Map<String, Object> response = paymentService.createPaymentOrder(userId, upiId, amount);
        return ResponseEntity.ok(response);
    }

    /**
     * Step 2: Razorpay sends the secret mathematical signature here to prove success.
     * Note: This endpoint must NOT be protected by JWT because Razorpay (a server) calls it,
     * not the logged-in user! We will configure Spring Security to allow this later.
     */
    @PostMapping("/webhook")
    public ResponseEntity<?> razorpayWebhook(@RequestBody Map<String, String> payload) {
        String orderId = payload.get("razorpay_order_id");
        String paymentId = payload.get("razorpay_payment_id");
        String signature = payload.get("razorpay_signature");

        boolean isAuthentic = paymentService.verifyPaymentSignature(orderId, paymentId, signature);

        if (isAuthentic) {
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Payment verified!"));
        } else {
            // Hacker detected! Return a 400 Bad Request error.
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Invalid signature!"));
        }
    }

    /**
     * Generate Dynamic QR Code for Scan & Pay
     */
    @PostMapping("/qr/generate")
    public ResponseEntity<?> generateQrCode(@RequestBody Map<String, Object> requestData) {
        String amountStr = requestData.get("amount").toString();
        BigDecimal amount = new BigDecimal(amountStr);
        String description = (String) requestData.get("description");

        String qrResponse = paymentService.generateQrCode(amount, description);
        return ResponseEntity.ok(qrResponse);
    }

    /**
     * Initiate Bank Transfer (RazorpayX Payouts)
     */
    @PostMapping("/bank/transfer")
    public ResponseEntity<?> initiateBankTransfer(@RequestBody Map<String, Object> requestData) {
        String amountStr = requestData.get("amount").toString();
        BigDecimal amount = new BigDecimal(amountStr);
        String beneficiaryName = (String) requestData.get("beneficiaryName");
        String accountNumber = (String) requestData.get("accountNumber");
        String ifscCode = (String) requestData.get("ifscCode");
        String purpose = (String) requestData.get("purpose");

        String transferResponse = paymentService.initiateBankTransfer(amount, beneficiaryName, accountNumber, ifscCode, purpose);
        return ResponseEntity.ok(transferResponse);
    }
}
