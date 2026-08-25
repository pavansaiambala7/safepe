package com.safepe.fraud.repository;

import com.safepe.fraud.model.FraudPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FraudPatternRepository extends JpaRepository<FraudPattern, UUID> {
    List<FraudPattern> findByCategory(String category);
    List<FraudPattern> findBySeverity(String severity);
    List<FraudPattern> findByCategoryAndSeverity(String category, String severity);
}
