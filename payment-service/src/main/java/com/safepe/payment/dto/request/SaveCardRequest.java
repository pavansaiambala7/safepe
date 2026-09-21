package com.safepe.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body for POST /api/v1/vault/cards.
 * <p>
 * Only the last four digits are ever persisted - see TokenizationService. The
 * full number is validated for shape so a typo fails here rather than becoming
 * a token that masks to the wrong digits.
 */
public record SaveCardRequest(

        @NotBlank(message = "cardNumber is required")
        @Pattern(regexp = "^[0-9]{12,19}$", message = "must be 12 to 19 digits")
        String cardNumber,

        @Size(max = 100, message = "must be at most 100 characters")
        String razorpayTokenId
) {}
