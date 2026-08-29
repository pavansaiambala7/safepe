# SafePe — Execution Prompt #3: Bill / EMI / Recharge Reminder Events (Kafka + SSE)

**Paste this whole document to your Antigravity agent. Execute in order, build, then push. Base commit: `c723586`.**

## What we're building
A **time-triggered** reminder pipeline (no user click): a scheduled job in **payment-service** finds bills that are due soon, publishes a reminder to a new Kafka topic `bill-reminders`, and **notification-service** consumes it and rings the SSE bell — "💳 Your HDFC Credit Card bill of ₹12,300 is due in 3 days."

Flow:
```
@Scheduled job (payment-service, daily 9AM)
   └─ query scheduled_bills due within 3 days
        └─ publish BillReminderEvent → Kafka topic "bill-reminders"
             └─ notification-service @KafkaListener
                  └─ SSE push → 🔔 bell
```
For live demos we also add a **manual trigger endpoint** so you don't wait until 9 AM.

---

## PART A — payment-service: data + producer + scheduler

### A1. Enable scheduling
`payment-service/.../PaymentServiceApplication.java` — add `@EnableScheduling`:
```java
import org.springframework.scheduling.annotation.EnableScheduling;
// ...
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class PaymentServiceApplication { ... }
```

### A2. New entity `model/ScheduledBill.java`
```java
package com.safepe.payment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "scheduled_bills")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ScheduledBill {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "type", length = 20, nullable = false)   // CC_BILL | EMI | RECHARGE
    private String type;

    @Column(name = "payee_name", length = 120)
    private String payeeName;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Builder.Default
    @Column(name = "status", length = 20)
    private String status = "PENDING";      // PENDING | NOTIFIED | PAID

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
```

### A3. New repo `repository/ScheduledBillRepository.java`
```java
package com.safepe.payment.repository;

import com.safepe.payment.model.ScheduledBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledBillRepository extends JpaRepository<ScheduledBill, UUID> {
    List<ScheduledBill> findByUserId(String userId);
    List<ScheduledBill> findByStatus(String status);
}
```

### A4. New DTO `dto/BillReminderEvent.java`
```java
package com.safepe.payment.dto;

import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BillReminderEvent implements Serializable {
    private String userId;
    private String type;
    private String payeeName;
    private BigDecimal amount;
    private LocalDate dueDate;
    private long daysUntilDue;
}
```

### A5. New topic constant + bean in `config/KafkaConfig.java`
Add alongside the existing topic (and delete the unused `FRAUD_ALERTS_TOPIC` constant while you're here):
```java
public static final String BILL_REMINDERS_TOPIC = "bill-reminders";

@Bean
public NewTopic billRemindersTopic() {
    return TopicBuilder.name(BILL_REMINDERS_TOPIC).partitions(3).replicas(1).build();
}
```

### A6. New producer `service/BillReminderProducer.java`
```java
package com.safepe.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safepe.payment.config.KafkaConfig;
import com.safepe.payment.dto.BillReminderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BillReminderProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public BillReminderProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public void publish(BillReminderEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaConfig.BILL_REMINDERS_TOPIC, event.getUserId(), payload);
            log.info("⏰ Published BillReminderEvent: user={}, {} ₹{} due in {}d",
                    event.getUserId(), event.getType(), event.getAmount(), event.getDaysUntilDue());
        } catch (Exception e) {
            log.error("Failed to publish bill reminder: {}", e.getMessage());
        }
    }
}
```

### A7. New scheduler `service/BillReminderScheduler.java`
```java
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
```

### A8. New controller `controller/BillController.java`
Lets you create demo bills and trigger the sweep on demand.
```java
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
```

### A9. (Optional) seeder `config/ScheduledBillSeeder.java`
Seeds three demo bills due in 1–3 days for a configurable user, so the demo has data on a fresh DB. Set the user via `safepe.demo.user-id` (defaults to `demo_user`). For the live EC2 demo, either set that property to your real Clerk user id, or just create bills through `POST /api/v1/bills`.
```java
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
        LocalDate today = LocalDate.now();
        billRepository.saveAll(List.of(
            ScheduledBill.builder().userId(demoUserId).type("CC_BILL").payeeName("HDFC Credit Card")
                .amount(new BigDecimal("12300")).dueDate(today.plusDays(3)).status("PENDING").build(),
            ScheduledBill.builder().userId(demoUserId).type("EMI").payeeName("Bajaj Finserv EMI")
                .amount(new BigDecimal("4500")).dueDate(today.plusDays(2)).status("PENDING").build(),
            ScheduledBill.builder().userId(demoUserId).type("RECHARGE").payeeName("Airtel Prepaid")
                .amount(new BigDecimal("299")).dueDate(today.plusDays(1)).status("PENDING").build()
        ));
        log.info("🌱 Seeded 3 demo scheduled bills for user {}", demoUserId);
    }
}
```

---

## PART B — notification-service: consume `bill-reminders` → ring the bell

### B1. New DTO `dto/BillReminderEvent.java` (mirror payment-service's)
```java
package com.safepe.notification.dto;

import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BillReminderEvent implements Serializable {
    private String userId;
    private String type;
    private String payeeName;
    private BigDecimal amount;
    private LocalDate dueDate;
    private long daysUntilDue;
}
```

### B2. Topic constant in `config/KafkaConfig.java`
```java
public static final String BILL_REMINDERS_TOPIC = "bill-reminders";
```

### B3. New consumer `service/BillReminderConsumer.java`
Reuses the existing `NotificationSSEService.broadcast(NotificationEvent)` and the existing `NotificationEvent` shape (fields: id, type, title, message, amount, upiId, referenceId, transactionId, timestamp).
```java
package com.safepe.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safepe.notification.config.KafkaConfig;
import com.safepe.notification.dto.BillReminderEvent;
import com.safepe.notification.dto.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class BillReminderConsumer {

    private final NotificationSSEService sseService;
    private final ObjectMapper objectMapper;

    public BillReminderConsumer(NotificationSSEService sseService) {
        this.sseService = sseService;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @KafkaListener(topics = KafkaConfig.BILL_REMINDERS_TOPIC, groupId = "safepe-notification-group")
    public void consume(String message) {
        try {
            BillReminderEvent e = objectMapper.readValue(message, BillReminderEvent.class);

            String icon = switch (e.getType() == null ? "" : e.getType()) {
                case "CC_BILL" -> "💳";
                case "EMI" -> "📆";
                case "RECHARGE" -> "📱";
                default -> "🔔";
            };
            String when = e.getDaysUntilDue() == 0 ? "today"
                        : "in " + e.getDaysUntilDue() + " day" + (e.getDaysUntilDue() == 1 ? "" : "s");

            sseService.broadcast(NotificationEvent.builder()
                    .id("notif-reminder-" + UUID.randomUUID().toString().substring(0, 8))
                    .type("REMINDER")
                    .title("Bill Due Soon")
                    .message(String.format("%s Your %s of ₹%s is due %s.",
                            icon, e.getPayeeName(), e.getAmount(), when))
                    .amount(e.getAmount())
                    .referenceId(e.getType())
                    .timestamp(LocalDateTime.now())
                    .build());

            log.info("🔔 Bill reminder pushed to SSE for user {}", e.getUserId());
        } catch (Exception ex) {
            log.error("Failed to process bill reminder: {}", ex.getMessage());
        }
    }
}
```
> If `NotificationEvent.type` has a comment restricting values to SUCCESS/SECURITY, `"REMINDER"` is still just a String — no enum, so it's fine. Make sure the frontend bell renders unknown types with a default style (see Part D).

---

## PART C — API Gateway route for `/bills`
`api-gateway/src/main/resources/application.yml` — add under the Payment Service routes group:
```yaml
        - id: payment-service-bills
          uri: lb://payment-service
          predicates:
            - Path=/api/v1/bills/**
```

---

## PART D — Frontend (optional but recommended for the demo)
The bell already renders `NotificationEvent`s, so reminders show up automatically. Two small touches:
1. In the bell component (`frontend/src/components/NotificationBell.tsx`), make sure a notification with `type === "REMINDER"` renders with a sensible icon/color (add a case; fall back to a neutral style for unknown types so nothing breaks).
2. (Nice-to-have) A tiny **Bills** demo control — a button that calls `POST /api/v1/bills/run-reminders` so you can trigger the bell live on stage. You can drop it on the Dashboard or add a `/bills` page. Minimal version:
```jsx
const triggerReminders = async () => {
  const token = await getToken();
  await api.post('/bills/run-reminders', {}, { headers: { Authorization: `Bearer ${token}` } });
};
```
Add a matching sidebar link only if you make a full page.

---

## BUILD, VERIFY, DEMO, PUSH
```bash
mvn -q -DskipTests clean package
docker compose build --no-cache
docker compose up -d
```
Live smoke test (replace <UID> with your Clerk user id from the browser, or use demo_user):
```bash
# create a bill due in 2 days
curl -s -X POST http://localhost:8080/api/v1/bills \
  -H "Content-Type: application/json" \
  -d '{"userId":"<UID>","type":"CC_BILL","payeeName":"HDFC Credit Card","amount":"12300","dueDate":"'$(date -d "+2 days" +%F)'"}'

# open the app + the bell, then fire reminders:
curl -s -X POST http://localhost:8080/api/v1/bills/run-reminders
# → {"remindersSent":N} and the 🔔 bell should light up in the browser
```
Confirm in logs: `docker compose logs payment-service | grep "Published BillReminderEvent"` and `docker compose logs notification-service | grep "Bill reminder pushed"`.

Then:
```bash
git add -A
git commit -m "feat: scheduled bill/EMI/recharge reminders via Kafka bill-reminders topic + SSE bell"
git push origin main
```
Redeploy on EC2: `git pull origin main && docker compose build --no-cache && docker compose up -d`

---

## Interview talking point (say this unprompted)
"Reminders are **time-triggered**, not user-triggered, so they belong on a queue: the `@Scheduled` sweep doesn't know or care which user sessions are connected — it just drops a `BillReminderEvent` on the `bill-reminders` topic and walks away; notification-service owns delivery. The one production gap is that `@Scheduled` fires on every instance, so at scale I'd add a distributed lock (ShedLock) or a dedicated scheduler so reminders aren't sent twice."
