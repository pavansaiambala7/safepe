package com.safepe.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fraud Pattern Entity — RAG Vector Store
 * =========================================
 * Stores known fraud patterns with their text descriptions and
 * pre-computed vector embeddings (768 dimensions from Gemini text-embedding-004).
 * Used by the RAG pipeline for semantic similarity search via pgvector.
 *
 * Categories: PHISHING, UPI_FRAUD, LOAN_SCAM, KYC_FRAUD, LOTTERY_SCAM,
 *             INVESTMENT_FRAUD, IMPERSONATION, VISHING
 */
@Entity
@Table(name = "fraud_patterns")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "pattern_description", length = 2000, nullable = false)
    private String patternDescription;

    @Column(name = "category", length = 50, nullable = false)
    private String category;

    @Column(name = "severity", length = 20, nullable = false)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "keywords", length = 500)
    private String keywords;

    @Builder.Default
    @Column(name = "match_count")
    private Integer matchCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
