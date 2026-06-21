package com.safepe.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpendSummaryResponse {
    private BigDecimal totalSpent;
    private BigDecimal totalBudget;
    private Integer transactionCount;
    private Map<String, BigDecimal> categoryBreakdown;
    private String monthYear;
}
