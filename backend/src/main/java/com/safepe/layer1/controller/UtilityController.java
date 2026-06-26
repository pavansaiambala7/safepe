package com.safepe.layer1.controller;

import com.safepe.service.BBPSService;
import com.safepe.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * BBPS Utility Controller — Bills & Recharges
 * =============================================
 * Provides REST endpoints for the frontend utility pages:
 *   - GET  /billers?category=mobile      → List operators
 *   - GET  /plans?operator=JIO           → List recharge plans
 *   - POST /bill/fetch                   → Fetch outstanding bill
 *   - POST /order/create                 → Create Razorpay Order
 *   - POST /order/verify                 → Verify payment + fulfill
 *   - GET  /balance                      → Check linked bank balance
 */
@RestController
@RequestMapping("/api/v1/utilities")
@RequiredArgsConstructor
@Slf4j
public class UtilityController {

    private final BBPSService bbpsService;
    private final PaymentService paymentService;

    // ─── 1. List Billers/Operators ──────────────────────────────────────
    @GetMapping("/billers")
    public ResponseEntity<?> getBillers(@RequestParam String category) {
        log.info("📋 GET /billers?category={}", category);
        List<Map<String, Object>> billers = bbpsService.getBillers(category);
        return ResponseEntity.ok(Map.of("billers", billers, "count", billers.size()));
    }

    // ─── 2. List Recharge Plans ─────────────────────────────────────────
    @GetMapping("/plans")
    public ResponseEntity<?> getPlans(@RequestParam String operator) {
        log.info("📱 GET /plans?operator={}", operator);
        List<Map<String, Object>> plans = bbpsService.getRechargePlans(operator);
        return ResponseEntity.ok(Map.of("plans", plans, "operator", operator));
    }

    // ─── 3. Fetch Outstanding Bill (Electricity/Postpaid) ───────────────
    @PostMapping("/bill/fetch")
    public ResponseEntity<?> fetchBill(@RequestBody Map<String, String> request) {
        String billerId = request.get("biller_id");
        String customerIdentifier = request.get("customer_identifier");
        log.info("🔍 POST /bill/fetch biller={}, customer={}", billerId, customerIdentifier);

        Map<String, Object> bill = bbpsService.fetchBill(billerId, customerIdentifier);
        return ResponseEntity.ok(bill);
    }

    // ─── 4. Create Razorpay Order (REAL PAYMENT) ────────────────────────
    @PostMapping("/order/create")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> request) {
        String billerId = (String) request.get("biller_id");
        String customerIdentifier = (String) request.get("customer_identifier");
        double amount = Double.parseDouble(request.get("amount").toString());
        String category = (String) request.get("category");

        log.info("💳 POST /order/create biller={}, amount=₹{}", billerId, amount);

        Map<String, Object> order = bbpsService.createBillPaymentOrder(
                billerId, customerIdentifier, amount, category);
        return ResponseEntity.ok(order);
    }

    // ─── 5. Verify Payment + Fulfill Recharge/Bill ──────────────────────
    @PostMapping("/order/verify")
    public ResponseEntity<?> verifyAndFulfill(@RequestBody Map<String, String> request) {
        String orderId = request.get("razorpay_order_id");
        String paymentId = request.get("razorpay_payment_id");
        String signature = request.get("razorpay_signature");
        String billerId = request.get("biller_id");
        String customerIdentifier = request.get("customer_identifier");
        double amount = Double.parseDouble(request.get("amount"));
        String category = request.get("category");

        log.info("🔐 POST /order/verify order={}", orderId);

        // Step 1: Verify payment signature with Razorpay
        boolean isVerified = paymentService.verifyPaymentSignature(orderId, paymentId, signature);

        if (isVerified) {
            // Step 2: Fulfill the recharge/bill payment via BBPS
            Map<String, Object> result = bbpsService.fulfillPayment(
                    orderId, paymentId, billerId, customerIdentifier, amount, category);
            return ResponseEntity.ok(result);
        } else {
            log.warn("🚨 Payment verification FAILED for order={}", orderId);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "message", "Payment verification failed. Possible tampered request."
            ));
        }
    }

    // ─── 6. Check Bank Balance ──────────────────────────────────────────
    @GetMapping("/balance")
    public ResponseEntity<?> getBankBalance() {
        log.info("💰 GET /balance");

        // When RazorpayX is activated, replace with real balance API call:
        // GET https://api.razorpay.com/v1/balance
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "balance", 45250.75,
                "currency", "INR",
                "accountName", "HDFC Bank (SafePe Linked)"
        ));
    }
}
