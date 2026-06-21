package com.safepe.dto;

/**
 * Request DTO for merchant fraud-check lookups.
 *
 * @param merchantName display name of the merchant to verify
 * @param upiId        optional UPI VPA (e.g. merchant@upi) for more precise lookup
 */
public record FraudCheckRequest(
        String merchantName,
        String upiId
) {}
