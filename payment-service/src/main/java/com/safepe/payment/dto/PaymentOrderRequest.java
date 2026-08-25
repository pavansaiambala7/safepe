package com.safepe.payment.dto;

import java.math.BigDecimal;

public record PaymentOrderRequest(
        BigDecimal amount,
        String purpose
) {}
