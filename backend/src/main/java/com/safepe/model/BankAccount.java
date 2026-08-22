package com.safepe.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bank_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", length = 255, nullable = false)
    private String userId;

    @Column(name = "bank_name", length = 100, nullable = false)
    private String bankName;

    @Column(name = "razorpay_token_id", length = 50)
    private String razorpayTokenId;
    
    // UI display friendly info
    @Column(name = "account_last_four", length = 4)
    private String accountLastFour;

    @Column(name = "balance", precision = 12, scale = 2, nullable = false)
    private BigDecimal balance;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
