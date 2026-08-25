package com.safepe.fraud.dto;

public record FDRecommendation(
        String bankName,
        Double rate,
        Integer tenureMonths,
        String reasoning
) {}
