package com.safepe.payment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "scheduled_bills")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledBill {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "type", length = 20, nullable = false)   // CC_BILL | EMI | RECHARGE
    private String type;

    @Column(name = "payee_name", length = 120)
    private String payeeName;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Builder.Default
    @Column(name = "status", length = 20)
    private String status = "PENDING";      // PENDING | NOTIFIED | PAID

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
