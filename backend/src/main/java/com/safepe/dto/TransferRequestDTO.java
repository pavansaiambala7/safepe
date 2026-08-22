package com.safepe.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRequestDTO {
    private BigDecimal amount;
    private String beneficiaryName;
    private String accountNumber;
    private String ifscCode;
    private String purpose;
}
