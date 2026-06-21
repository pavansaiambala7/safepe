package com.safepe.layer3.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Layer 3 — Encrypted Vault entity for UPI IDs.
 * UPI handles are stored encrypted; a SHA-256 hash enables lookups.
 * The masked UPI (e.g. "r***@oksbi") is safe for display.
 */
@Entity
@Table(name = "encrypted_upi_ids")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncryptedUPI {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "upi_hash", unique = true, nullable = false)
    private String upiHash; // SHA-256 hash for lookup

    @Lob
    @Column(name = "encrypted_upi", nullable = false)
    private byte[] encryptedUpi; // AES-256-GCM encrypted UPI handle

    @Column(name = "masked_upi", length = 50)
    private String maskedUpi; // e.g. "r***@oksbi"

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
