package com.safepe.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent implements Serializable {
    private String eventType;
    private String userId;
    private String serviceSource;
    private String action;
    private String details;
    private String ipAddress;
    private String status;
    private LocalDateTime timestamp;
}
