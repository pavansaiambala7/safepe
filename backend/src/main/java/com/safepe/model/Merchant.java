package com.safepe.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Layer 1/2 — Merchant entity.
 * Tracks merchant trust scores, verification status, and fraud flags.
 * Trust score ranges from 0.00 (untrusted) to 1.00 (fully trusted).
 */
@Entity
@Table(name = "merchants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "trust_score", nullable = false)
    private Double trustScore; // 0.00 to 1.00

    @Builder.Default
    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Builder.Default
    @Column(name = "is_flagged")
    private Boolean isFlagged = false;

    @Column(name = "flag_reason", length = 500)
    private String flagReason;

    @Builder.Default
    @Column(name = "report_count")
    private Integer reportCount = 0;

    @Column(name = "upi_id_masked", length = 50)
    private String upiIdMasked;

    @Column(name = "website", length = 300)
    private String website;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
