package com.safepe.fraud.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Local read model of payment activity, projected from the Kafka
 * transaction-events topic by PaymentEventConsumer.
 * <p>
 * Table renamed from "transactions" to "fraud_transaction_view": that name was
 * also mapped by payment-service with a DIFFERENT column set (it has
 * razorpay_payment_id, this does not). Two services with ddl-auto=update
 * pointed at one table meant whichever booted last reshaped it, and the schema
 * depended on container start order. This service owns the projection; it does
 * not own the source of truth.
 */
@Entity
@Table(name = "fraud_transaction_view")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Transaction {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", length = 255, nullable = false)
    private String userId;

    @Column(name = "payee_upi", length = 255)
    private String payeeUpi;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Builder.Default
    @Column(name = "currency", length = 3)
    private String currency = "INR";

    @Column(name = "type", length = 20, nullable = false)
    private String type;

    @Builder.Default
    @Column(name = "status", length = 20)
    private String status = "PENDING";

    @Column(name = "razorpay_order_id", length = 100)
    private String razorpayOrderId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
