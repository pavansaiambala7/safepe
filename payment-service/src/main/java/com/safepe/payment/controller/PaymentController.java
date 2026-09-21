package com.safepe.payment.controller;

import com.safepe.payment.dto.request.BankTransferRequest;
import com.safepe.payment.dto.request.CreatePaymentRequest;
import com.safepe.payment.dto.request.GenerateQrRequest;
import com.safepe.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreatePaymentRequest request,
                                         Principal principal) {
        String userId = principal != null ? principal.getName() : "user_123_temp";
        Map<String, Object> response = paymentService.createPaymentOrder(
                userId, request.upiId(), request.amount());
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
    public ResponseEntity<?> generateQrCode(@Valid @RequestBody GenerateQrRequest request) {
        String qrResponse = paymentService.generateQrCode(
                request.amount(), request.description());
        return ResponseEntity.ok(qrResponse);
    }

    @PostMapping("/bank/transfer")
    public ResponseEntity<?> initiateBankTransfer(@Valid @RequestBody BankTransferRequest request) {
        String transferResponse = paymentService.initiateBankTransfer(
                request.amount(), request.beneficiaryName(), request.accountNumber(),
                request.ifscCode(), request.purpose());
        return ResponseEntity.ok(transferResponse);
    }
}
