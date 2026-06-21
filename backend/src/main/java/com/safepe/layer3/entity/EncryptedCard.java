package com.safepe.layer3.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Layer 3 — Encrypted Vault entity for card data.
 * Card details are stored as AES-256-GCM encrypted JSON blobs.
 * A SHA-256 hash of the card number enables lookups without decryption.
 */
@Entity
@Table(name = "encrypted_cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncryptedCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId; // Clerk user ID

    @Column(name = "card_hash", unique = true, nullable = false)
    private String cardHash; // SHA-256 hash for deduplication & lookup

    @Lob
    @Column(name = "encrypted_data", nullable = false)
    private byte[] encryptedData; // AES-256-GCM encrypted JSON

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
