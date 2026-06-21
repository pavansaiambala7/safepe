package com.safepe.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Layer 1/2 — Budget entity.
 * Tracks per-category monthly spending limits and current expenditure.
 * Unique constraint ensures one budget per user/category/month combination.
 */
@Entity
@Table(
    name = "budgets",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_budget_user_category_month",
        columnNames = {"user_id", "category", "month_year"}
    )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", length = 255, nullable = false)
    private String userId;

    @Column(name = "category", length = 100, nullable = false)
    private String category;

    @Column(name = "monthly_limit", precision = 12, scale = 2, nullable = false)
    private BigDecimal monthlyLimit;

    @Builder.Default
    @Column(name = "current_spent", precision = 12, scale = 2)
    private BigDecimal currentSpent = BigDecimal.ZERO;

    @Column(name = "month_year", length = 7, nullable = false)
    private String monthYear; // Format: "2026-06"

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
