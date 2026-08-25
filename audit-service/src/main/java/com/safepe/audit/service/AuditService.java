package com.safepe.audit.service;

import com.safepe.audit.dto.AuditEvent;
import com.safepe.audit.model.AuditLog;
import com.safepe.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public AuditLog recordAudit(AuditEvent event) {
        AuditLog logEntry = AuditLog.builder()
                .eventType(event.getEventType())
                .userId(event.getUserId())
                .serviceSource(event.getServiceSource() != null ? event.getServiceSource() : "system")
                .action(event.getAction())
                .details(event.getDetails())
                .ipAddress(event.getIpAddress())
                .status(event.getStatus() != null ? event.getStatus() : "RECORDED")
                .timestamp(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now())
                .build();

        AuditLog saved = auditLogRepository.save(logEntry);
        log.info("📝 [Audit Trail] Recorded audit log: type={}, user={}, action={}",
                saved.getEventType(), saved.getUserId(), saved.getAction());
        return saved;
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop100ByOrderByTimestampDesc();
    }

    public List<AuditLog> getLogsByUser(String userId) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    public List<AuditLog> getLogsByEventType(String eventType) {
        return auditLogRepository.findByEventTypeOrderByTimestampDesc(eventType);
    }
}
