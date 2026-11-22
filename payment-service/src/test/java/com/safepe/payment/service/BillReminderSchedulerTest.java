package com.safepe.payment.service;

import com.safepe.payment.dto.BillReminderEvent;
import com.safepe.payment.model.ScheduledBill;
import com.safepe.payment.repository.ScheduledBillRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BillReminderSchedulerTest {

    private final ScheduledBillRepository bills = mock(ScheduledBillRepository.class);
    private final BillReminderProducer producer = mock(BillReminderProducer.class);
    private final BillReminderScheduler scheduler = new BillReminderScheduler(bills, producer);

    private static ScheduledBill billDueIn(int days, String user) {
        return ScheduledBill.builder().userId(user).type("CC_BILL").payeeName("HDFC Card")
                .amount(new BigDecimal("1200.00")).dueDate(LocalDate.now().plusDays(days)).status("PENDING").build();
    }

    @Test
    void remindsOnlyBillsDueWithinThreeDays() {
        when(bills.findByStatus("PENDING")).thenReturn(List.of(
                billDueIn(-1, "overdue"), billDueIn(0, "today"), billDueIn(3, "in3"), billDueIn(4, "in4")));

        int sent = scheduler.runReminders(false);

        ArgumentCaptor<BillReminderEvent> events = ArgumentCaptor.forClass(BillReminderEvent.class);
        verify(producer, times(2)).publish(events.capture());
        assertThat(sent).isEqualTo(2);
        assertThat(events.getAllValues()).extracting(BillReminderEvent::getUserId).containsExactly("today", "in3");
        assertThat(events.getAllValues()).extracting(BillReminderEvent::getDaysUntilDue).containsExactly(0L, 3L);
    }

    @Test
    void dailySweepMarksBillsNotifiedSoTheyAreNotRepeated() {
        ScheduledBill bill = billDueIn(1, "user_a");
        when(bills.findByStatus("PENDING")).thenReturn(List.of(bill));

        scheduler.runReminders(true);

        assertThat(bill.getStatus()).isEqualTo("NOTIFIED");
        verify(bills).save(bill);
    }

    @Test
    void manualRunLeavesBillsPendingSoItCanBeReplayed() {
        ScheduledBill bill = billDueIn(1, "user_a");
        when(bills.findByStatus("PENDING")).thenReturn(List.of(bill));

        scheduler.runReminders(false);

        assertThat(bill.getStatus()).isEqualTo("PENDING");
        verify(bills, never()).save(any());
    }
}
