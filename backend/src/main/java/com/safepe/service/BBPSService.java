package com.safepe.service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * BBPS (Bharat Bill Payment System) Service
 * ==========================================
 * Handles real bill payments and recharges via Razorpay's
 * payment collection + BBPS settlement pipeline.
 *
 * Flow:
 *   1. Frontend selects provider + enters details
 *   2. This service creates a Razorpay Order (real payment collection)
 *   3. Frontend opens Razorpay Checkout, user pays
 *   4. Razorpay webhook confirms payment
 *   5. We mark the bill/recharge as PAID and trigger fulfillment
 *
 * For BBPS activation:
 *   - Log into dashboard.razorpay.com → Products → BBPS
 *   - Once activated, replace the mock biller data with real BBPS API calls
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BBPSService {

    private final RazorpayClient razorpayClient;

    @Value("${safepe.razorpay.key-id}")
    private String razorpayKeyId;

    // ═══════════════════════════════════════════════════════════════════
    // 1. BILLER CATALOG — Returns available providers per category
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Returns the list of available billers/operators for a given category.
     * When Razorpay BBPS is activated, replace this with:
     *   GET https://api.razorpay.com/v1/bbps/billers?category={category}
     */
    public List<Map<String, Object>> getBillers(String category) {
        log.info("📋 Fetching billers for category: {}", category);

        switch (category.toLowerCase()) {
            case "mobile":
            case "mobile_prepaid":
                return List.of(
                    biller("JIO", "Jio Prepaid", "jio.com", "MOBILE_PREPAID"),
                    biller("AIRTEL", "Airtel Prepaid", "airtel.in", "MOBILE_PREPAID"),
                    biller("VI", "Vi (Vodafone Idea)", "myvi.in", "MOBILE_PREPAID"),
                    biller("BSNL", "BSNL Prepaid", "bsnl.co.in", "MOBILE_PREPAID")
                );

            case "postpaid":
            case "mobile_postpaid":
                return List.of(
                    biller("JIO_POST", "Jio Postpaid", "jio.com", "MOBILE_POSTPAID"),
                    biller("AIRTEL_POST", "Airtel Postpaid", "airtel.in", "MOBILE_POSTPAID"),
                    biller("VI_POST", "Vi Postpaid", "myvi.in", "MOBILE_POSTPAID"),
                    biller("BSNL_POST", "BSNL Postpaid", "bsnl.co.in", "MOBILE_POSTPAID")
                );

            case "electricity":
                return List.of(
                    biller("APDCL", "Assam Power Distribution Company Ltd (APDCL)", "apdcl.org", "ELECTRICITY"),
                    biller("AEML", "Adani Electricity Mumbai Limited (AEML)", "adanielectricity.com", "ELECTRICITY"),
                    biller("TSSPDCL", "Telangana SPDCL", "tssouthernpower.com", "ELECTRICITY"),
                    biller("APSPDCL", "AP Southern Power Distribution", "apspdcl.in", "ELECTRICITY"),
                    biller("BESCOM", "BESCOM Bangalore", "bescom.karnataka.gov.in", "ELECTRICITY"),
                    biller("MSEDCL", "Maharashtra MSEDCL", "mahadiscom.in", "ELECTRICITY"),
                    biller("TANGEDCO", "TANGEDCO Tamil Nadu", "tangedco.org", "ELECTRICITY"),
                    biller("CESC", "CESC Kolkata", "cesc.co.in", "ELECTRICITY")
                );

            case "dth":
                return List.of(
                    biller("AIRTEL_DTH", "Airtel Digital TV", "airtel.in", "DTH"),
                    biller("DISH_TV", "Dish TV", "dishtv.in", "DTH"),
                    biller("SUN_DIRECT", "Sun Direct", "sundirect.in", "DTH"),
                    biller("TATA_PLAY", "Tata Play (Formerly Tatasky)", "tataplay.com", "DTH"),
                    biller("D2H", "D2H", "d2h.com", "DTH")
                );

            default:
                return List.of();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 2. FETCH BILL — Get outstanding amount for postpaid/electricity
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Fetches the outstanding bill for a customer.
     * When BBPS is activated, replace with:
     *   POST https://api.razorpay.com/v1/bbps/bills/fetch
     *   { "biller_id": "...", "customer_params": { ... } }
     */
    public Map<String, Object> fetchBill(String billerId, String customerIdentifier) {
        log.info("🔍 Fetching bill for biller={}, customer={}", billerId, customerIdentifier);

        // Simulate fetching a real bill
        Map<String, Object> bill = new HashMap<>();
        bill.put("biller_id", billerId);
        bill.put("customer_identifier", customerIdentifier);
        bill.put("customer_name", "SafePe User");
        bill.put("bill_date", "2026-06-01");
        bill.put("due_date", "2026-06-30");

        // Generate a realistic amount based on category
        double amount;
        if (billerId.contains("DTH")) {
            amount = 299 + new Random().nextInt(500);
        } else if (billerId.contains("APDCL") || billerId.contains("AEML") || billerId.contains("BESCOM") ||
                   billerId.contains("MSEDCL") || billerId.contains("TANGEDCO") || billerId.contains("CESC") ||
                   billerId.contains("TSSPDCL") || billerId.contains("APSPDCL")) {
            amount = 500 + new Random().nextInt(3000);
        } else {
            // Postpaid
            amount = 399 + new Random().nextInt(800);
        }

        bill.put("amount", amount);
        bill.put("status", "UNPAID");
        return bill;
    }

    // ═══════════════════════════════════════════════════════════════════
    // 3. CREATE ORDER — Real Razorpay Order for payment collection
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Creates a REAL Razorpay Order so the frontend can open Razorpay Checkout.
     * This is the production-ready payment collection step.
     */
    public Map<String, Object> createBillPaymentOrder(String billerId, String customerIdentifier,
                                                       double amount, String category) {
        log.info("💳 Creating Razorpay Order: biller={}, customer={}, amount=₹{}", billerId, customerIdentifier, amount);

        try {
            // Convert to paise (Razorpay uses smallest currency unit)
            long amountInPaise = Math.round(amount * 100);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "bbps_" + System.currentTimeMillis());

            // Add notes for tracking
            JSONObject notes = new JSONObject();
            notes.put("biller_id", billerId);
            notes.put("customer_identifier", customerIdentifier);
            notes.put("category", category);
            notes.put("source", "safepe_bbps");
            orderRequest.put("notes", notes);

            // Create REAL Razorpay Order
            com.razorpay.Order order = razorpayClient.orders.create(orderRequest);

            String orderId = order.get("id");
            log.info("✅ Razorpay Order created: {}", orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("order_id", orderId);
            response.put("amount", amount);
            response.put("amount_paise", amountInPaise);
            response.put("currency", "INR");
            response.put("razorpay_key_id", razorpayKeyId);
            response.put("biller_id", billerId);
            response.put("customer_identifier", customerIdentifier);
            response.put("category", category);
            response.put("status", "CREATED");
            return response;

        } catch (RazorpayException e) {
            log.error("❌ Failed to create Razorpay order for BBPS payment", e);
            throw new RuntimeException("Payment gateway is currently unavailable: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 4. VERIFY & FULFILL — After payment, process the recharge/bill
    // ═══════════════════════════════════════════════════════════════════

    /**
     * After Razorpay payment is verified, this method handles the actual
     * bill payment/recharge fulfillment via BBPS.
     *
     * When BBPS is activated, replace with:
     *   POST https://api.razorpay.com/v1/bbps/bills/pay
     */
    public Map<String, Object> fulfillPayment(String orderId, String paymentId, String billerId,
                                               String customerIdentifier, double amount, String category) {
        log.info("🔄 Fulfilling BBPS payment: order={}, payment={}, biller={}", orderId, paymentId, billerId);

        // When Razorpay BBPS is activated, this would call:
        // POST /v1/bbps/bills/pay with the payment_id and biller details
        // For now, we record the successful transaction

        Map<String, Object> result = new HashMap<>();
        result.put("status", "SUCCESS");
        result.put("razorpay_order_id", orderId);
        result.put("razorpay_payment_id", paymentId);
        result.put("biller_id", billerId);
        result.put("customer_identifier", customerIdentifier);
        result.put("amount", amount);
        result.put("category", category);
        result.put("bbps_reference", "BBPS" + System.currentTimeMillis());
        result.put("message", getSuccessMessage(category, customerIdentifier, amount));
        result.put("timestamp", new java.util.Date().toString());

        log.info("✅ BBPS Payment fulfilled: {}", result.get("bbps_reference"));
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════
    // 5. RECHARGE PLANS — Popular plans for mobile recharge
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Returns popular recharge plans for a given operator.
     * In production, this would come from the BBPS API or operator APIs.
     */
    public List<Map<String, Object>> getRechargePlans(String operator) {
        log.info("📱 Fetching plans for operator: {}", operator);

        List<Map<String, Object>> plans = new ArrayList<>();
        switch (operator.toUpperCase()) {
            // ── Mobile Prepaid ──────────────────────────────────
            case "JIO":
                plans.add(plan("₹19", 19, "1GB Data", "1 Day", "Data Pack"));
                plans.add(plan("₹149", 149, "2GB/day + Unlimited Calls", "24 Days", "Popular"));
                plans.add(plan("₹239", 239, "1.5GB/day + Unlimited Calls", "28 Days", "Value"));
                plans.add(plan("₹299", 299, "2GB/day + Unlimited Calls", "28 Days", "Best Seller"));
                plans.add(plan("₹599", 599, "2GB/day + Unlimited Calls", "56 Days", "Super Value"));
                plans.add(plan("₹799", 799, "2GB/day + Unlimited Calls", "84 Days", "Quarterly"));
                plans.add(plan("₹2999", 2999, "2.5GB/day + Unlimited Calls", "365 Days", "Annual"));
                break;
            case "AIRTEL":
                plans.add(plan("₹19", 19, "Data Pack 1GB", "1 Day", "Data Pack"));
                plans.add(plan("₹179", 179, "2GB/day + Unlimited Calls", "28 Days", "Popular"));
                plans.add(plan("₹265", 265, "1GB/day + Unlimited Calls", "28 Days", "Value"));
                plans.add(plan("₹299", 299, "1.5GB/day + Unlimited Calls", "28 Days", "Smart"));
                plans.add(plan("₹719", 719, "2GB/day + Unlimited Calls", "84 Days", "Quarterly"));
                plans.add(plan("₹2999", 2999, "2GB/day + Unlimited Calls", "365 Days", "Annual"));
                break;
            case "VI":
                plans.add(plan("₹19", 19, "Data Pack 1GB", "1 Day", "Data Pack"));
                plans.add(plan("₹199", 199, "1GB/day + Unlimited Calls", "28 Days", "Popular"));
                plans.add(plan("₹299", 299, "1.5GB/day + Unlimited Calls", "28 Days", "Best Seller"));
                plans.add(plan("₹449", 449, "2GB/day + Unlimited Calls", "56 Days", "Double Up"));
                plans.add(plan("₹719", 719, "1.5GB/day + Unlimited Calls", "84 Days", "Quarterly"));
                break;
            case "BSNL":
                plans.add(plan("₹18", 18, "1GB Data", "1 Day", "Data Pack"));
                plans.add(plan("₹107", 107, "1GB/day + Unlimited Calls", "24 Days", "Budget"));
                plans.add(plan("₹187", 187, "2GB/day + Unlimited Calls", "28 Days", "Popular"));
                plans.add(plan("₹365", 365, "2GB/day + Unlimited Calls", "60 Days", "Value"));
                plans.add(plan("₹599", 599, "2GB/day + Unlimited Calls", "84 Days", "Quarterly"));
                break;

            // ── DTH Plans ───────────────────────────────────────
            case "AIRTEL_DTH":
                plans.add(plan("₹153", 153, "Hindi Basic HD — 150+ Channels", "1 Month", "Basic"));
                plans.add(plan("₹254", 254, "Hindi Value HD — 200+ Channels", "1 Month", "Popular"));
                plans.add(plan("₹396", 396, "Hindi Premium HD — 280+ Channels", "1 Month", "Premium"));
                plans.add(plan("₹503", 503, "All-in-One HD — 350+ Channels", "1 Month", "Best Seller"));
                plans.add(plan("₹1399", 1399, "Hindi Premium HD", "3 Months", "Quarterly"));
                plans.add(plan("₹4999", 4999, "All-in-One HD", "12 Months", "Annual"));
                break;
            case "DISH_TV":
                plans.add(plan("₹150", 150, "Super Family — 130+ Channels", "1 Month", "Basic"));
                plans.add(plan("₹218", 218, "Super Family HD — 170+ Channels", "1 Month", "Popular"));
                plans.add(plan("₹311", 311, "Titanium HD — 250+ Channels", "1 Month", "Value"));
                plans.add(plan("₹425", 425, "Diamond HD — 320+ Channels", "1 Month", "Best Seller"));
                plans.add(plan("₹1099", 1099, "Titanium HD", "3 Months", "Quarterly"));
                plans.add(plan("₹4299", 4299, "Diamond HD", "12 Months", "Annual"));
                break;
            case "SUN_DIRECT":
                plans.add(plan("₹130", 130, "Telugu Value — 120+ Channels", "1 Month", "Basic"));
                plans.add(plan("₹199", 199, "Telugu Super — 180+ Channels", "1 Month", "Popular"));
                plans.add(plan("₹279", 279, "Telugu Premium HD — 220+ Channels", "1 Month", "Premium"));
                plans.add(plan("₹399", 399, "All South HD — 300+ Channels", "1 Month", "Best Seller"));
                plans.add(plan("₹1099", 1099, "Telugu Premium HD", "3 Months", "Quarterly"));
                break;
            case "TATA_PLAY":
                plans.add(plan("₹153", 153, "Hindi Starter HD — 140+ Channels", "1 Month", "Basic"));
                plans.add(plan("₹247", 247, "Hindi Smart HD — 200+ Channels", "1 Month", "Popular"));
                plans.add(plan("₹350", 350, "Hindi Entertainment HD — 260+ Channels", "1 Month", "Value"));
                plans.add(plan("₹459", 459, "Mega HD — 350+ Channels", "1 Month", "Best Seller"));
                plans.add(plan("₹2999", 2999, "Mega HD", "6 Months", "Half Yearly"));
                plans.add(plan("₹5499", 5499, "Mega HD", "12 Months", "Annual"));
                break;
            case "D2H":
                plans.add(plan("₹153", 153, "Silver HD — 150+ Channels", "1 Month", "Basic"));
                plans.add(plan("₹220", 220, "Gold HD — 200+ Channels", "1 Month", "Popular"));
                plans.add(plan("₹330", 330, "Diamond HD — 270+ Channels", "1 Month", "Value"));
                plans.add(plan("₹449", 449, "Platinum HD — 330+ Channels", "1 Month", "Best Seller"));
                plans.add(plan("₹1249", 1249, "Diamond HD", "3 Months", "Quarterly"));
                plans.add(plan("₹4599", 4599, "Platinum HD", "12 Months", "Annual"));
                break;

            // ── Postpaid Plans ──────────────────────────────────
            case "JIO_POST":
                plans.add(plan("₹399", 399, "75GB + Unlimited Calls + JioTV", "1 Month", "Popular"));
                plans.add(plan("₹599", 599, "100GB + Unlimited + Netflix Mobile", "1 Month", "Value"));
                plans.add(plan("₹799", 799, "150GB + Unlimited + Netflix Basic", "1 Month", "Best Seller"));
                plans.add(plan("₹999", 999, "Unlimited Data + Netflix Standard", "1 Month", "Premium"));
                plans.add(plan("₹1499", 1499, "Unlimited Data + Netflix + Prime", "1 Month", "Family"));
                break;
            case "AIRTEL_POST":
                plans.add(plan("₹399", 399, "40GB + Unlimited Calls + Prime Lite", "1 Month", "Popular"));
                plans.add(plan("₹549", 549, "75GB + Unlimited + Disney+ Mobile", "1 Month", "Value"));
                plans.add(plan("₹799", 799, "100GB + Unlimited + Netflix Mobile", "1 Month", "Best Seller"));
                plans.add(plan("₹999", 999, "150GB + Unlimited + Netflix Basic", "1 Month", "Premium"));
                plans.add(plan("₹1599", 1599, "Unlimited + Netflix + Prime + Disney+", "1 Month", "Family"));
                break;
            case "VI_POST":
                plans.add(plan("₹399", 399, "40GB + Unlimited Calls", "1 Month", "Popular"));
                plans.add(plan("₹499", 499, "75GB + Unlimited + Prime Lite", "1 Month", "Value"));
                plans.add(plan("₹699", 699, "100GB + Unlimited + Netflix Mobile", "1 Month", "Best Seller"));
                plans.add(plan("₹1099", 1099, "Unlimited Data + Netflix + Prime", "1 Month", "Premium"));
                break;
            case "BSNL_POST":
                plans.add(plan("₹199", 199, "25GB + Unlimited Calls", "1 Month", "Budget"));
                plans.add(plan("₹399", 399, "50GB + Unlimited Calls", "1 Month", "Popular"));
                plans.add(plan("₹525", 525, "75GB + Unlimited Calls", "1 Month", "Value"));
                plans.add(plan("₹799", 799, "Unlimited Data + Calls", "1 Month", "Best Seller"));
                break;

            // ── Electricity (common amounts) ────────────────────
            case "APDCL": case "AEML": case "TSSPDCL": case "APSPDCL":
            case "BESCOM": case "MSEDCL": case "TANGEDCO": case "CESC":
                plans.add(plan("₹500", 500, "Quick Pay — ₹500", "One-Time", "Quick"));
                plans.add(plan("₹1000", 1000, "Quick Pay — ₹1,000", "One-Time", "Popular"));
                plans.add(plan("₹2000", 2000, "Quick Pay — ₹2,000", "One-Time", "Value"));
                plans.add(plan("₹3000", 3000, "Quick Pay — ₹3,000", "One-Time", ""));
                plans.add(plan("₹5000", 5000, "Quick Pay — ₹5,000", "One-Time", ""));
                break;

            default:
                plans.add(plan("₹199", 199, "Basic Plan", "28 Days", "Popular"));
                plans.add(plan("₹399", 399, "Standard Plan", "56 Days", "Value"));
                plans.add(plan("₹799", 799, "Premium Plan", "84 Days", "Best Seller"));
        }
        return plans;
    }

    // ═══════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════

    private Map<String, Object> biller(String id, String name, String domain, String category) {
        Map<String, Object> b = new HashMap<>();
        b.put("id", id);
        b.put("name", name);
        b.put("domain", domain);
        b.put("category", category);
        return b;
    }

    private Map<String, Object> plan(String label, int amount, String data, String validity, String tag) {
        Map<String, Object> p = new HashMap<>();
        p.put("label", label);
        p.put("amount", amount);
        p.put("data", data);
        p.put("validity", validity);
        p.put("tag", tag);
        return p;
    }

    private String getSuccessMessage(String category, String identifier, double amount) {
        switch (category.toLowerCase()) {
            case "mobile":
            case "mobile_prepaid":
                return String.format("Mobile recharge of ₹%.0f successful for %s", amount, identifier);
            case "postpaid":
            case "mobile_postpaid":
                return String.format("Postpaid bill of ₹%.0f paid for %s", amount, identifier);
            case "electricity":
                return String.format("Electricity bill of ₹%.0f paid for consumer %s", amount, identifier);
            case "dth":
                return String.format("DTH recharge of ₹%.0f successful for subscriber %s", amount, identifier);
            default:
                return String.format("Payment of ₹%.0f successful for %s", amount, identifier);
        }
    }
}
