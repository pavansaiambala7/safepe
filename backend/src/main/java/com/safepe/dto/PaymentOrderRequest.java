package com.safepe.dto;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new Razorpay payment order.
 *
 * @param amount  payment amount in INR (rupees, not paise)
 * @param purpose human-readable description of what the payment is for
 */
public record PaymentOrderRequest(
        BigDecimal amount,
        String purpose
) {}
