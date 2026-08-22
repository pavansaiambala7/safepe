package com.safepe.repository;

import com.safepe.model.FraudPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Fraud Pattern Repository
 * =========================
 * Provides CRUD and query operations for fraud patterns.
 * Vector similarity search is handled separately by LangChain4j's
 * PgVectorEmbeddingStore for cosine distance operations.
 */
@Repository
public interface FraudPatternRepository extends JpaRepository<FraudPattern, UUID> {

    List<FraudPattern> findByCategory(String category);

    List<FraudPattern> findBySeverity(String severity);

    List<FraudPattern> findByCategoryAndSeverity(String category, String severity);
}
