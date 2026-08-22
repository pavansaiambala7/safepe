package com.safepe.layer3.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Layer 3 — Tokenized entity for card data.
 * PCI DSS Compliant: Stores Razorpay Network Tokens instead of raw card numbers.
 */
@Entity
@Table(name = "tokenized_cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenizedCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId; // Clerk user ID

    @Column(name = "razorpay_customer_id", nullable = false)
    private String razorpayCustomerId; 

    @Column(name = "razorpay_token_id", unique = true, nullable = false)
    private String razorpayTokenId; 

    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    @Column(name = "card_network", length = 20)
    private String cardNetwork; // VISA, MASTERCARD, RUPAY

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
