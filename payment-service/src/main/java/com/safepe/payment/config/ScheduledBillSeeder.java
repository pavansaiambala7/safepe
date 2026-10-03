package com.safepe.payment.config;

import com.safepe.payment.model.ScheduledBill;
import com.safepe.payment.repository.ScheduledBillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledBillSeeder implements CommandLineRunner {

    private final ScheduledBillRepository billRepository;

    @Value("${safepe.demo.user-id:demo_user}")
    private String demoUserId;

    @Override
    public void run(String... args) {
        if (billRepository.count() > 0) return;
        seedFor(demoUserId);
    }

    /** Saves the three demo bills (due in 1-3 days) for the given user. */
    public void seedFor(String userId) {
        LocalDate today = LocalDate.now();
        billRepository.saveAll(List.of(
            ScheduledBill.builder().userId(userId).type("CC_BILL").payeeName("HDFC Credit Card")
                .amount(new BigDecimal("12300")).dueDate(today.plusDays(3)).status("PENDING").build(),
            ScheduledBill.builder().userId(userId).type("EMI").payeeName("Bajaj Finserv EMI")
                .amount(new BigDecimal("4500")).dueDate(today.plusDays(2)).status("PENDING").build(),
            ScheduledBill.builder().userId(userId).type("RECHARGE").payeeName("Airtel Prepaid")
                .amount(new BigDecimal("299")).dueDate(today.plusDays(1)).status("PENDING").build()
        ));
        log.info("🌱 Seeded 3 demo scheduled bills for user {}", userId);
    }
}
