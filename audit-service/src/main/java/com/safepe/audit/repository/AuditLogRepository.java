package com.safepe.audit.repository;

import com.safepe.audit.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByUserIdOrderByTimestampDesc(String userId);
    List<AuditLog> findByEventTypeOrderByTimestampDesc(String eventType);
    List<AuditLog> findTop100ByOrderByTimestampDesc();
}
