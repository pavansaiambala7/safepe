package com.safepe.fraud.dto;

import java.math.BigDecimal;

public record BudgetRequest(
        String category,
        BigDecimal monthlyLimit,
        String monthYear
) {}
