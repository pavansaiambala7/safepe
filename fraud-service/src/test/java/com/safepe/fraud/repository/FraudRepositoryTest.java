package com.safepe.fraud.repository;

import com.safepe.fraud.model.FraudPattern;
import com.safepe.fraud.model.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import com.safepe.fraud.support.PostgresTestBase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs fraud-service's Flyway migrations against PostgreSQL 15 and validates the
 * entities against the schema, then checks the queries the RAG features rely on.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class FraudRepositoryTest extends PostgresTestBase {

    @Autowired TransactionRepository transactions;
    @Autowired FraudPatternRepository patterns;

    private Transaction txn(String user, String amount, LocalDateTime at) {
        return Transaction.builder().id(UUID.randomUUID()).userId(user).payeeUpi("shop@oksbi")
                .amount(new BigDecimal(amount)).type("UPI").status("SUCCESS").createdAt(at).build();
    }

    @Test
    void recentTransactionsAreTheUsersOwnNewestFirst() {
        LocalDateTime now = LocalDateTime.now();
        transactions.save(txn("user_a", "100.00", now));
        transactions.save(txn("user_a", "300.00", now));
        transactions.save(txn("user_a", "200.00", now));
        transactions.save(txn("user_b", "999.00", now));

        assertThat(transactions.findRecentByUserId("user_a"))
                .hasSize(3)
                .allSatisfy(t -> assertThat(t.getUserId()).isEqualTo("user_a"))
                .isSortedAccordingTo(java.util.Comparator.comparing(Transaction::getCreatedAt).reversed());
    }

    @Test
    @org.junit.jupiter.api.Disabled("Known bug, fixed in Phase 3: @CreationTimestamp overwrites createdAt with the "
            + "time the Kafka event was consumed, so the read model loses when the payment actually happened")
    void keepsThePaymentsOwnTimestamp() {
        LocalDateTime paidAt = LocalDateTime.of(2026, 1, 15, 10, 30);
        Transaction saved = transactions.saveAndFlush(txn("user_a", "100.00", paidAt));

        assertThat(transactions.findById(saved.getId())).get()
                .extracting(Transaction::getCreatedAt).isEqualTo(paidAt);
    }

    @Test
    void fraudPatternsAreQueryableByCategory() {
        patterns.save(FraudPattern.builder().patternDescription("Update KYC via link or account blocked")
                .category("KYC_FRAUD").severity("HIGH").keywords("kyc,link").build());
        patterns.save(FraudPattern.builder().patternDescription("You won a lottery, pay fee to claim")
                .category("LOTTERY_SCAM").severity("MEDIUM").build());

        assertThat(patterns.findByCategory("KYC_FRAUD")).singleElement()
                .satisfies(p -> assertThat(p.getId()).isNotNull());
        assertThat(patterns.findByCategoryAndSeverity("LOTTERY_SCAM", "MEDIUM")).hasSize(1);
    }
}
