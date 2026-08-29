package com.safepe.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safepe.notification.config.KafkaConfig;
import com.safepe.notification.dto.BillReminderEvent;
import com.safepe.notification.dto.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class BillReminderConsumer {

    private final NotificationSSEService sseService;
    private final ObjectMapper objectMapper;

    public BillReminderConsumer(NotificationSSEService sseService) {
        this.sseService = sseService;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @KafkaListener(topics = KafkaConfig.BILL_REMINDERS_TOPIC, groupId = "safepe-notification-group")
    public void consume(String message) {
        try {
            BillReminderEvent e = objectMapper.readValue(message, BillReminderEvent.class);

            String icon = switch (e.getType() == null ? "" : e.getType()) {
                case "CC_BILL" -> "💳";
                case "EMI" -> "📆";
                case "RECHARGE" -> "📱";
                default -> "🔔";
            };
            String when = e.getDaysUntilDue() == 0 ? "today"
                        : "in " + e.getDaysUntilDue() + " day" + (e.getDaysUntilDue() == 1 ? "" : "s");

            sseService.broadcast(NotificationEvent.builder()
                    .id("notif-reminder-" + UUID.randomUUID().toString().substring(0, 8))
                    .type("REMINDER")
                    .title("Bill Due Soon")
                    .message(String.format("%s Your %s of ₹%s is due %s.",
                            icon, e.getPayeeName(), e.getAmount(), when))
                    .amount(e.getAmount())
                    .referenceId(e.getType())
                    .timestamp(LocalDateTime.now())
                    .build());

            log.info("🔔 Bill reminder pushed to SSE for user {}", e.getUserId());
        } catch (Exception ex) {
            log.error("Failed to process bill reminder: {}", ex.getMessage());
        }
    }
}
