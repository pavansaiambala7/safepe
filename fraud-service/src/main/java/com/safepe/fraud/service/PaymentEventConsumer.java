package com.safepe.fraud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safepe.fraud.config.KafkaConfig;
import com.safepe.fraud.dto.TransactionEvent;
import com.safepe.fraud.model.Transaction;
import com.safepe.fraud.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @KafkaListener(topics = KafkaConfig.TRANSACTION_EVENTS_TOPIC, groupId = "safepe-ai-readmodel-group")
    public void consume(String message) {
        try {
            TransactionEvent e = objectMapper.readValue(message, TransactionEvent.class);
            Transaction t = Transaction.builder()
                    .id(e.getTransactionId())
                    .userId(e.getUserId())
                    .payeeUpi(e.getUpiId())
                    .amount(e.getAmount())
                    .currency(e.getCurrency() != null ? e.getCurrency() : "INR")
                    .type(e.getType() != null ? e.getType() : "UPI")
                    .status("SUCCESS")
                    .razorpayOrderId(e.getRazorpayOrderId())
                    .build();
            transactionRepository.save(t);
            log.info("🧾 Read-model saved txn {} for user {}", e.getTransactionId(), e.getUserId());
        } catch (Exception ex) {
            log.error("Failed to save read-model transaction: {}", ex.getMessage());
        }
    }
}
