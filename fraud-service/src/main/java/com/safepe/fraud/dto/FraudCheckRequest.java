package com.safepe.fraud.dto;

public record FraudCheckRequest(
        String merchantName,
        String upiId
) {}
