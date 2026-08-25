package com.safepe.payment.controller;

import com.safepe.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> requestData, Principal principal) {
        String userId = principal != null ? principal.getName() : "user_123_temp";
        String upiId = (String) requestData.get("upiId");
        String amountStr = requestData.get("amount").toString();
        BigDecimal amount = new BigDecimal(amountStr);

        Map<String, Object> response = paymentService.createPaymentOrder(userId, upiId, amount);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> razorpayWebhook(@RequestBody Map<String, String> payload) {
        String orderId = payload.get("razorpay_order_id");
        String paymentId = payload.get("razorpay_payment_id");
        String signature = payload.get("razorpay_signature");

        boolean isAuthentic = paymentService.verifyPaymentSignature(orderId, paymentId, signature);

        if (isAuthentic) {
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Payment verified!"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Invalid signature!"));
        }
    }

    @PostMapping("/qr/generate")
    public ResponseEntity<?> generateQrCode(@RequestBody Map<String, Object> requestData) {
        String amountStr = requestData.get("amount").toString();
        BigDecimal amount = new BigDecimal(amountStr);
        String description = (String) requestData.get("description");

        String qrResponse = paymentService.generateQrCode(amount, description);
        return ResponseEntity.ok(qrResponse);
    }

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
