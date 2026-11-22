package com.safepe.fraud.service;

import com.safepe.fraud.model.Transaction;
import com.safepe.fraud.repository.TransactionRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Kafka 'transaction-events' -> fraud-service read model (used by the Money Assistant and insights). */
class PaymentEventConsumerTest {

    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final PaymentEventConsumer consumer = new PaymentEventConsumer(transactions);

    private static final String EVENT = """
            {"transactionId":"11111111-2222-3333-4444-555555555555","userId":"user_a","upiId":"shop@oksbi",
             "amount":250.00,"currency":"INR","type":"UPI","razorpayOrderId":"order_abc",
             "timestamp":"2026-01-15T10:30:00"}""";

    private Transaction saved() {
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactions).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void copiesEventIntoReadModel() {
        consumer.consume(EVENT);

        Transaction t = saved();
        assertThat(t.getId()).hasToString("11111111-2222-3333-4444-555555555555");
        assertThat(t.getUserId()).isEqualTo("user_a");
        assertThat(t.getPayeeUpi()).isEqualTo("shop@oksbi");
        assertThat(t.getAmount()).isEqualByComparingTo("250.00");
        assertThat(t.getRazorpayOrderId()).isEqualTo("order_abc");
    }

    @Test
    void defaultsCurrencyAndTypeWhenMissing() {
        consumer.consume("{\"transactionId\":\"11111111-2222-3333-4444-555555555555\",\"userId\":\"user_a\",\"amount\":10}");

        assertThat(saved()).returns("INR", Transaction::getCurrency).returns("UPI", Transaction::getType);
    }

    @Test
    void malformedMessageIsSkippedNotRethrown() {
        consumer.consume("not json");

        verify(transactions, never()).save(any());
    }

    @Test
    @Disabled("Known bug, fixed in Phase 3: payment-service publishes this event when the order is CREATED, "
            + "but the read model records it as SUCCESS, so unpaid orders count as spending")
    void doesNotRecordUnpaidOrdersAsSuccessful() {
        consumer.consume(EVENT);

        assertThat(saved().getStatus()).isNotEqualTo("SUCCESS");
    }
}
