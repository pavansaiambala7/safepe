package com.safepe.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequestDTO {
    private BigDecimal amount;
    private String currency = "INR";
    private String description;
}
