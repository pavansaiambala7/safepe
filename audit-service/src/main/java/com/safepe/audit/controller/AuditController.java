package com.safepe.audit.controller;

import com.safepe.audit.dto.AuditEvent;
import com.safepe.audit.model.AuditLog;
import com.safepe.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/logs")
    public ResponseEntity<List<AuditLog>> getRecentLogs() {
        return ResponseEntity.ok(auditService.getRecentLogs());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLog>> getLogsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(auditService.getLogsByUser(userId));
    }

    @GetMapping("/events/{eventType}")
    public ResponseEntity<List<AuditLog>> getLogsByEventType(@PathVariable String eventType) {
        return ResponseEntity.ok(auditService.getLogsByEventType(eventType));
    }

    @PostMapping("/log")
    public ResponseEntity<AuditLog> recordAudit(@RequestBody AuditEvent event) {
        return ResponseEntity.ok(auditService.recordAudit(event));
    }
}
