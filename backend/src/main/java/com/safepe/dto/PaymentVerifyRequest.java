package com.safepe.dto;

/**
 * Request DTO for server-side Razorpay payment signature verification.
 *
 * @param razorpayOrderId   the order_id returned during order creation
 * @param razorpayPaymentId the pay_id returned by the checkout widget
 * @param razorpaySignature HMAC-SHA256 signature to verify authenticity
 */
public record PaymentVerifyRequest(
        String razorpayOrderId,
        String razorpayPaymentId,
        String razorpaySignature
) {}
