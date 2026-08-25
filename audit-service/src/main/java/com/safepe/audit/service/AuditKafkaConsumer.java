package com.safepe.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safepe.audit.config.KafkaConfig;
import com.safepe.audit.dto.AuditEvent;
import com.safepe.audit.dto.FraudAlertEvent;
import com.safepe.audit.dto.TransactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class AuditKafkaConsumer {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public AuditKafkaConsumer(AuditService auditService) {
        this.auditService = auditService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @KafkaListener(topics = KafkaConfig.AUDIT_EVENTS_TOPIC, groupId = "safepe-audit-group")
    public void consumeAuditEvent(String message) {
        try {
            AuditEvent event = objectMapper.readValue(message, AuditEvent.class);
            auditService.recordAudit(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AuditEvent from Kafka: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = KafkaConfig.TRANSACTION_EVENTS_TOPIC, groupId = "safepe-audit-group")
    public void consumeTransactionEvent(String message) {
        try {
            TransactionEvent event = objectMapper.readValue(message, TransactionEvent.class);
            AuditEvent audit = AuditEvent.builder()
                    .eventType("TRANSACTION_INITIATED")
                    .userId(event.getUserId())
                    .serviceSource("payment-service")
                    .action("PAYMENT_ATTEMPT")
                    .details(String.format("TxnId=%s, Amount=%s %s, UPI=%s, Merchant=%s",
                            event.getTransactionId(), event.getAmount(), event.getCurrency(),
                            event.getUpiId(), event.getMerchantName()))
                    .status("SUCCESS")
                    .timestamp(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now())
                    .build();

            auditService.recordAudit(audit);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse TransactionEvent in Audit Consumer: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = KafkaConfig.FRAUD_ALERTS_TOPIC, groupId = "safepe-audit-group")
    public void consumeFraudAlert(String message) {
        try {
            FraudAlertEvent alert = objectMapper.readValue(message, FraudAlertEvent.class);
            String eventType = "BLOCK".equalsIgnoreCase(alert.getAction())
                    ? "FRAUD_SECURITY_BLOCK"
                    : ("FLAG_VERIFICATION".equalsIgnoreCase(alert.getAction()) ? "FRAUD_FLAGGED_WARNING" : "TRANSACTION_CLEARED");

            AuditEvent audit = AuditEvent.builder()
                    .eventType(eventType)
                    .userId(alert.getUserId())
                    .serviceSource("fraud-service")
                    .action(alert.getAction())
                    .details(String.format("TxnId=%s, RiskScore=%d%%, Summary=%s",
                            alert.getTransactionId(), alert.getRiskScore(), alert.getSummary()))
                    .status(alert.getAction())
                    .timestamp(alert.getTimestamp() != null ? alert.getTimestamp() : LocalDateTime.now())
                    .build();

            auditService.recordAudit(audit);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse FraudAlertEvent in Audit Consumer: {}", e.getMessage());
        }
    }
}
