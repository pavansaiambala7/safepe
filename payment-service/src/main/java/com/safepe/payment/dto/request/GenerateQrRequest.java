package com.safepe.payment.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Body for POST /api/v1/payments/qr/generate.
 * Description is optional; it is echoed into the QR payload, so it is length
 * capped rather than left unbounded.
 */
public record GenerateQrRequest(

        @NotNull(message = "amount is required")
        @DecimalMin(value = "1.00", message = "must be at least 1.00")
        @DecimalMax(value = "500000.00", message = "must not exceed 500000.00")
        BigDecimal amount,

        @Size(max = 120, message = "must be at most 120 characters")
        String description
) {}
