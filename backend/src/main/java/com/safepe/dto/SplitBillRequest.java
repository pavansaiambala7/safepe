package com.safepe.dto;

import java.math.BigDecimal;
import java.util.List;

public record SplitBillRequest(
    String title,
    BigDecimal totalAmount,
    List<String> participantUserIds
) {}
