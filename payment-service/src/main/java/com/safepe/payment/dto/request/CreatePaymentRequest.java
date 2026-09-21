package com.safepe.payment.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Replaces the untyped Map body on POST /api/v1/payments/create.
 * <p>
 * The previous code read {@code requestData.get("amount").toString()}, which
 * threw NullPointerException when the key was absent and surfaced to the caller
 * as a 500. Binding to a record makes a missing or malformed field a 400 with
 * the offending field named, handled by GlobalExceptionHandler.
 */
public record CreatePaymentRequest(

        @NotNull(message = "upiId is required")
        @Pattern(regexp = "^[\\w.\\-]{2,256}@[a-zA-Z]{2,64}$",
                 message = "must be a valid UPI id, for example name@bank")
        String upiId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "1.00", message = "must be at least 1.00")
        @DecimalMax(value = "500000.00", message = "must not exceed 500000.00")
        BigDecimal amount
) {}
