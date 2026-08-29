package com.safepe.payment.service;

import com.safepe.payment.dto.BillReminderEvent;
import com.safepe.payment.model.ScheduledBill;
import com.safepe.payment.repository.ScheduledBillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillReminderScheduler {

    private static final int REMINDER_WINDOW_DAYS = 3;

    private final ScheduledBillRepository billRepository;
    private final BillReminderProducer producer;

    /** Runs automatically every day at 09:00. Marks bills NOTIFIED so they don't repeat. */
    @Scheduled(cron = "0 0 9 * * *")
    public void dailyReminderSweep() {
        int sent = runReminders(true);
        log.info("📅 Daily reminder sweep complete — {} reminders sent", sent);
    }

    /**
     * Core logic. markNotified=true flips status so daily runs don't duplicate.
     * The manual demo endpoint calls it with false so it can be replayed.
     */
    public int runReminders(boolean markNotified) {
        LocalDate today = LocalDate.now();
        List<ScheduledBill> pending = billRepository.findByStatus("PENDING");
        int count = 0;

        for (ScheduledBill bill : pending) {
            long days = ChronoUnit.DAYS.between(today, bill.getDueDate());
            if (days < 0 || days > REMINDER_WINDOW_DAYS) continue;   // only due within the window

            producer.publish(BillReminderEvent.builder()
                    .userId(bill.getUserId())
                    .type(bill.getType())
                    .payeeName(bill.getPayeeName())
                    .amount(bill.getAmount())
                    .dueDate(bill.getDueDate())
                    .daysUntilDue(days)
                    .build());

            if (markNotified) {
                bill.setStatus("NOTIFIED");
                billRepository.save(bill);
            }
            count++;
        }
        return count;
    }
}
