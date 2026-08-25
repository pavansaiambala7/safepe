package com.safepe.payment.dto;

public record PaymentVerifyRequest(
        String razorpayOrderId,
        String razorpayPaymentId,
        String razorpaySignature
) {}
