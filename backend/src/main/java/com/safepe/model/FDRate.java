package com.safepe.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Layer 1/2 — Fixed Deposit Rate entity.
 * Stores FD rates from various banks for comparison.
 * Supports general and senior citizen rates, tenure ranges, and special schemes.
 */
@Entity
@Table(name = "fd_rates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FDRate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "bank_name", length = 200, nullable = false)
    private String bankName;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "general_rate", nullable = false)
    private Double generalRate;

    @Column(name = "senior_rate")
    private Double seniorRate;

    @Column(name = "min_amount", precision = 12, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "max_amount", precision = 12, scale = 2)
    private BigDecimal maxAmount;

    @Column(name = "special_scheme", length = 200)
    private String specialScheme;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}
