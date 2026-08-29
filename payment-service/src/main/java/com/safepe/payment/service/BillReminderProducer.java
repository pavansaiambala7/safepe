package com.safepe.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safepe.payment.config.KafkaConfig;
import com.safepe.payment.dto.BillReminderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BillReminderProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public BillReminderProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public void publish(BillReminderEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaConfig.BILL_REMINDERS_TOPIC, event.getUserId(), payload);
            log.info("⏰ Published BillReminderEvent: user={}, {} ₹{} due in {}d",
                    event.getUserId(), event.getType(), event.getAmount(), event.getDaysUntilDue());
        } catch (Exception e) {
            log.error("Failed to publish bill reminder: {}", e.getMessage());
        }
    }
}
