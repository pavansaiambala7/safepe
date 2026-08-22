package com.safepe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO returned after a Razorpay order is successfully created.
 * The frontend uses these fields to initialise the Razorpay checkout widget.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderResponse {

    /** Razorpay order ID (e.g. order_ABC123). */
    private String orderId;

    /** Order amount in INR. */
    private BigDecimal amount;

    /** ISO 4217 currency code (always "INR"). */
    private String currency;

    /** Razorpay API key-id — safe to expose to the browser. */
    private String keyId;
}
