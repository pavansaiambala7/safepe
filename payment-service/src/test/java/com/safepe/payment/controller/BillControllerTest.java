package com.safepe.payment.controller;

import com.safepe.payment.config.SecurityConfig;
import com.safepe.payment.model.ScheduledBill;
import com.safepe.payment.repository.ScheduledBillRepository;
import com.safepe.payment.service.BillReminderScheduler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BillController.class)
@Import(SecurityConfig.class)
@MockBean({ScheduledBillRepository.class, BillReminderScheduler.class})
class BillControllerTest {

    private final MockMvc mvc;
    private final ScheduledBillRepository bills;
    private final BillReminderScheduler scheduler;

    BillControllerTest(MockMvc mvc, ScheduledBillRepository bills, BillReminderScheduler scheduler) {
        this.mvc = mvc;
        this.bills = bills;
        this.scheduler = scheduler;
    }

    @Test
    void listsOnlyTheCallersBills() throws Exception {
        when(bills.findByUserId("user_a")).thenReturn(List.of(ScheduledBill.builder()
                .userId("user_a").type("EMI").payeeName("Bajaj").amount(new BigDecimal("5000.00"))
                .dueDate(LocalDate.of(2026, 12, 1)).status("PENDING").build()));

        mvc.perform(get("/api/v1/bills").header("X-User-Id", "user_a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].payeeName").value("Bajaj"));

        verify(bills).findByUserId("user_a");
    }

    @Test
    void createsBillOwnedByTheCallerEvenIfBodyNamesAnotherUser() throws Exception {
        when(bills.save(any(ScheduledBill.class))).thenAnswer(inv -> inv.getArgument(0));

        mvc.perform(post("/api/v1/bills").header("X-User-Id", "user_a").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": \"someone_else\", \"type\": \"CC_BILL\", \"payeeName\": \"HDFC\","
                                + " \"amount\": \"1200\", \"dueDate\": \"2026-12-01\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<ScheduledBill> saved = ArgumentCaptor.forClass(ScheduledBill.class);
        verify(bills).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo("user_a");
        assertThat(saved.getValue().getDueDate()).isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    void badDueDateIsAClientError() throws Exception {
        mvc.perform(post("/api/v1/bills").header("X-User-Id", "user_a").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": \"1200\", \"dueDate\": \"next tuesday\"}"))
                .andExpect(status().isBadRequest());

        verify(bills, never()).save(any());
    }

    @Test
    void manualReminderRunDoesNotMarkBillsNotified() throws Exception {
        when(scheduler.runReminders(false)).thenReturn(2);

        mvc.perform(post("/api/v1/bills/run-reminders").header("X-User-Id", "user_a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remindersSent").value(2));

        verify(scheduler).runReminders(false);
    }
}
