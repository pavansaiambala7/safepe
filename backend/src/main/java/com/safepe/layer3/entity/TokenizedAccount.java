package com.safepe.layer3.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Layer 3 — Tokenized entity for bank accounts.
 * PCI DSS Compliant: Stores Razorpay Network Tokens instead of raw account numbers.
 */
@Entity
@Table(name = "tokenized_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenizedAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "razorpay_customer_id", nullable = false)
    private String razorpayCustomerId;

    @Column(name = "razorpay_token_id", unique = true, nullable = false)
    private String razorpayTokenId; 

    @Column(name = "account_last_four", length = 4)
    private String accountLastFour;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "ifsc_code", length = 11)
    private String ifscCode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
