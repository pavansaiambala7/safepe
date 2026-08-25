package com.safepe.fraud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetResponse {
    private UUID id;
    private String category;
    private BigDecimal monthlyLimit;
    private BigDecimal currentSpent;
    private BigDecimal remaining;
    private Double percentUsed;
    private String monthYear;
}
