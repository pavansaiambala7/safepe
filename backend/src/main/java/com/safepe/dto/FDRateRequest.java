package com.safepe.dto;

import java.math.BigDecimal;

/**
 * Request DTO for AI-powered fixed-deposit rate comparison.
 *
 * @param amount        principal investment amount
 * @param tenureMonths  deposit tenure in months
 * @param seniorCitizen whether the depositor qualifies for senior-citizen rates
 */
public record FDRateRequest(
        BigDecimal amount,
        Integer tenureMonths,
        boolean seniorCitizen
) {}
