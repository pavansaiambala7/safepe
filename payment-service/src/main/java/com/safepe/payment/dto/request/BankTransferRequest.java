package com.safepe.payment.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Body for POST /api/v1/payments/bank/transfer.
 * <p>
 * The IFSC and account-number patterns are the real formats used by Indian
 * banks, so obviously malformed transfers are rejected before any provider
 * call is made.
 */
public record BankTransferRequest(

        @NotNull(message = "amount is required")
        @DecimalMin(value = "1.00", message = "must be at least 1.00")
        @DecimalMax(value = "500000.00", message = "must not exceed 500000.00")
        BigDecimal amount,

        @NotBlank(message = "beneficiaryName is required")
        @Size(max = 100, message = "must be at most 100 characters")
        String beneficiaryName,

        @NotBlank(message = "accountNumber is required")
        @Pattern(regexp = "^[0-9]{9,18}$", message = "must be 9 to 18 digits")
        String accountNumber,

        @NotBlank(message = "ifscCode is required")
        @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$",
                 message = "must be a valid IFSC code, for example HDFC0001234")
        String ifscCode,

        @Size(max = 120, message = "must be at most 120 characters")
        String purpose
) {}
