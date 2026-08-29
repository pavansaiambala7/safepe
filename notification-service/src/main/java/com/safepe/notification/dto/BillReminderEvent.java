package com.safepe.notification.dto;

import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillReminderEvent implements Serializable {
    private String userId;
    private String type;
    private String payeeName;
    private BigDecimal amount;
    private LocalDate dueDate;
    private long daysUntilDue;
}
