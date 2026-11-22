package com.safepe.notification.service;

import com.safepe.notification.dto.NotificationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Kafka message -> notification text, and which user it is addressed to. */
class NotificationConsumersTest {

    private final NotificationSSEService sse = mock(NotificationSSEService.class);

    private NotificationEvent sentTo(String userId) {
        ArgumentCaptor<NotificationEvent> event = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(sse).sendToUser(eq(userId), event.capture());
        return event.getValue();
    }

    @Test
    void paymentEventBecomesPaymentNotificationForThePayer() {
        new NotificationKafkaConsumer(sse).consumeTransactionEvent("""
                {"transactionId":"11111111-2222-3333-4444-555555555555","userId":"user_a",
                 "upiId":"shop@oksbi","amount":250.00}""");

        NotificationEvent n = sentTo("user_a");
        assertThat(n.getType()).isEqualTo("SUCCESS");
        assertThat(n.getMessage()).isEqualTo("Payment of ₹250.00 to shop@oksbi was successful.");
        assertThat(n.getTransactionId()).isEqualTo("11111111-2222-3333-4444-555555555555");
    }

    @ParameterizedTest(name = "{0} due in {1} day(s) -> \"{2}\"")
    @CsvSource(delimiter = '|', value = {
            "CC_BILL  | 0 | 💳 Your HDFC of ₹1200 is due today.",
            "EMI      | 1 | 📆 Your HDFC of ₹1200 is due in 1 day.",
            "RECHARGE | 3 | 📱 Your HDFC of ₹1200 is due in 3 days.",
            "OTHER    | 2 | 🔔 Your HDFC of ₹1200 is due in 2 days."
    })
    void billReminderTextDependsOnTypeAndDaysLeft(String type, int days, String expected) {
        new BillReminderConsumer(sse).consume("""
                {"userId":"user_b","type":"%s","payeeName":"HDFC","amount":1200,"dueDate":"2026-12-01","daysUntilDue":%d}"""
                .formatted(type, days));

        NotificationEvent n = sentTo("user_b");
        assertThat(n.getType()).isEqualTo("REMINDER");
        assertThat(n.getMessage()).isEqualTo(expected);
    }

    @Test
    void malformedMessagesAreDroppedWithoutThrowing() {
        new NotificationKafkaConsumer(sse).consumeTransactionEvent("not json");
        new BillReminderConsumer(sse).consume("{broken");

        verify(sse, never()).sendToUser(any(), any());
    }
}
