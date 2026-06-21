package com.safepe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO representing a single budget category with spend tracking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetResponse {

    private UUID id;

    /** Spending category (e.g. "Food", "Transport"). */
    private String category;

    /** Maximum monthly spend allowed. */
    private BigDecimal monthlyLimit;

    /** Amount already spent in this category this month. */
    private BigDecimal currentSpent;

    /** Remaining budget (monthlyLimit − currentSpent). */
    private BigDecimal remaining;

    /** Percentage of the budget consumed (0–100+). */
    private Double percentUsed;

    /** Target month in "MM-yyyy" format. */
    private String monthYear;
}
