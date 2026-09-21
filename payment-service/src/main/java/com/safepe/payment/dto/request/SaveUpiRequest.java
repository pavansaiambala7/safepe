package com.safepe.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body for POST /api/v1/vault/upi.
 * <p>
 * upiId was previously read straight out of a Map and could arrive null, in
 * which case maskUPI returned an empty string and an empty masked entry was
 * persisted.
 */
public record SaveUpiRequest(

        @NotBlank(message = "upiId is required")
        @Pattern(regexp = "^[\\w.\\-]{2,256}@[a-zA-Z]{2,64}$",
                 message = "must be a valid UPI id, for example name@bank")
        String upiId,

        @Size(max = 100, message = "must be at most 100 characters")
        String razorpayTokenId
) {}
