package com.safepe.payment.repository;

import com.safepe.payment.model.ScheduledBill;
import com.safepe.payment.model.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import com.safepe.payment.support.PostgresTestBase;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the real Flyway migrations against PostgreSQL 15 and lets Hibernate
 * validate the entities against the resulting schema (ddl-auto=validate), so
 * any drift between migrations and entities fails here instead of at startup
 * in production.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class PaymentRepositoryTest extends PostgresTestBase {

    @Autowired TransactionRepository transactions;
    @Autowired ScheduledBillRepository bills;

    @Test
    void savesTransactionAndFindsItByRazorpayOrderId() {
        Transaction saved = transactions.save(Transaction.builder()
                .userId("user_a").payeeUpi("shop@oksbi").amount(new BigDecimal("250.00"))
                .type("UPI").status("PENDING").razorpayOrderId("order_123").build());

        assertThat(saved.getId()).isNotNull();
        assertThat(transactions.findByRazorpayOrderId("order_123"))
                .singleElement()
                .satisfies(t -> {
                    assertThat(t.getUserId()).isEqualTo("user_a");
                    assertThat(t.getAmount()).isEqualByComparingTo("250.00");
                });
    }

    @Test
    void findsTransactionsOnlyForTheRequestedUser() {
        transactions.save(Transaction.builder().userId("user_a").amount(BigDecimal.TEN).type("UPI").status("SUCCESS").build());
        transactions.save(Transaction.builder().userId("user_b").amount(BigDecimal.ONE).type("UPI").status("SUCCESS").build());

        assertThat(transactions.findByUserId("user_a")).extracting(Transaction::getUserId).containsOnly("user_a");
        assertThat(transactions.findByUserIdAndStatus("user_b", "SUCCESS")).hasSize(1);
    }

    @Test
    void findsBillsByStatus() {
        bills.save(ScheduledBill.builder().userId("user_a").type("CC_BILL").payeeName("HDFC")
                .amount(new BigDecimal("1200.00")).dueDate(LocalDate.now().plusDays(2)).status("PENDING").build());
        bills.save(ScheduledBill.builder().userId("user_a").type("EMI").payeeName("Bajaj")
                .amount(new BigDecimal("5000.00")).dueDate(LocalDate.now().plusDays(1)).status("NOTIFIED").build());

        assertThat(bills.findByStatus("PENDING")).extracting(ScheduledBill::getType).containsExactly("CC_BILL");
    }
}
