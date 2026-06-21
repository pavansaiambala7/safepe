package com.safepe.dto;

import java.math.BigDecimal;

/**
 * Request DTO for creating or updating a monthly budget category.
 *
 * @param category     spending category (e.g. "Food", "Transport")
 * @param monthlyLimit maximum spend allowed for this category this month
 * @param monthYear    target month in "MM-yyyy" format (e.g. "06-2026")
 */
public record BudgetRequest(
        String category,
        BigDecimal monthlyLimit,
        String monthYear
) {}
