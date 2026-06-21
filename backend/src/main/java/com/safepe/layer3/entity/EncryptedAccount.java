package com.safepe.layer3.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Layer 3 — Encrypted Vault entity for bank accounts.
 * Full account details are AES-256-GCM encrypted; a SHA-256 hash enables lookups.
 * Non-sensitive metadata (last four, bank name, IFSC) is stored in cleartext for display.
 */
@Entity
@Table(name = "encrypted_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncryptedAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "account_hash", unique = true, nullable = false)
    private String accountHash; // SHA-256 hash for deduplication & lookup

    @Lob
    @Column(name = "encrypted_data", nullable = false)
    private byte[] encryptedData; // AES-256-GCM encrypted JSON

    @Column(name = "account_last_four", length = 4)
    private String accountLastFour;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "ifsc_code", length = 11)
    private String ifscCode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
