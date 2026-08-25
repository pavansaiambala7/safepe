package com.safepe.fraud.dto;

import java.math.BigDecimal;

public record FDRateRequest(
        BigDecimal amount,
        Integer tenureMonths,
        boolean seniorCitizen
) {}
