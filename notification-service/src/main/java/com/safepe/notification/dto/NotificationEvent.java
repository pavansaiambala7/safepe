package com.safepe.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {

    private String id;
    private String type;            // SUCCESS, SECURITY
    private String title;
    private String message;
    private BigDecimal amount;
    private String upiId;
    private String referenceId;
    private String transactionId;
    private LocalDateTime timestamp;
}
