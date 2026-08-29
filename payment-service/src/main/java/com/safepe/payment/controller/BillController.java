package com.safepe.payment.controller;

import com.safepe.payment.model.ScheduledBill;
import com.safepe.payment.repository.ScheduledBillRepository;
import com.safepe.payment.service.BillReminderScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bills")
@RequiredArgsConstructor
public class BillController {

    private final ScheduledBillRepository billRepository;
    private final BillReminderScheduler scheduler;

    @GetMapping
    public ResponseEntity<List<ScheduledBill>> myBills(@RequestParam String userId) {
        return ResponseEntity.ok(billRepository.findByUserId(userId));
    }

    @PostMapping
    public ResponseEntity<ScheduledBill> create(@RequestBody Map<String, String> body) {
        ScheduledBill bill = ScheduledBill.builder()
                .userId(body.getOrDefault("userId", "demo_user"))
                .type(body.getOrDefault("type", "CC_BILL"))
                .payeeName(body.getOrDefault("payeeName", "Credit Card"))
                .amount(new BigDecimal(body.getOrDefault("amount", "1000")))
                .dueDate(LocalDate.parse(body.getOrDefault("dueDate", LocalDate.now().plusDays(2).toString())))
                .status("PENDING")
                .build();
        return ResponseEntity.ok(billRepository.save(bill));
    }

    /** Demo trigger — publishes reminders for all due bills WITHOUT marking them notified, so it's replayable. */
    @PostMapping("/run-reminders")
    public ResponseEntity<?> runNow() {
        int sent = scheduler.runReminders(false);
        return ResponseEntity.ok(Map.of("remindersSent", sent));
    }
}
