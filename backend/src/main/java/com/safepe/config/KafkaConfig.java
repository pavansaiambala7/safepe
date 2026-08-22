package com.safepe.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka Configuration — Event-Driven Messaging
 * ==============================================
 * Configures Kafka topics for inter-service communication.
 * Topics:
 *   - transaction-events: Published when a payment is initiated
 *   - fraud-alerts: Published when the AI engine flags a transaction
 */
@Configuration
public class KafkaConfig {

    public static final String TRANSACTION_EVENTS_TOPIC = "transaction-events";
    public static final String FRAUD_ALERTS_TOPIC = "fraud-alerts";

    @Bean
    public NewTopic transactionEventsTopic() {
        return TopicBuilder.name(TRANSACTION_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fraudAlertsTopic() {
        return TopicBuilder.name(FRAUD_ALERTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
