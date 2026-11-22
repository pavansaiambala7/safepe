package com.safepe.notification;

import com.safepe.notification.config.KafkaConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End to end through real Kafka: events published to the topics reach the SSE
 * stream of the user they belong to, and nobody else's.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NotificationStreamIntegrationTest {

    /**
     * Started once and removed by Testcontainers' Ryuk after the JVM exits, rather
     * than stopped by @Container while Spring's cached context still has consumers
     * connected to it (which makes JVM shutdown hang).
     */
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.7.0");

    static {
        KAFKA.start();
    }

    @DynamicPropertySource
    static void kafka(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @LocalServerPort int port;
    @Autowired KafkaTemplate<String, String> kafka;

    private final HttpClient http = HttpClient.newHttpClient();
    private final List<java.util.concurrent.CompletableFuture<?>> openStreams = new ArrayList<>();

    @AfterEach
    void closeStreams() {
        openStreams.forEach(f -> f.cancel(true));
    }

    /** Opens the stream as {@code userId}; every received line is put on the returned queue. */
    private BlockingQueue<String> openStreamAs(String userId) throws InterruptedException {
        BlockingQueue<String> lines = new LinkedBlockingQueue<>();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/notifications/stream"))
                .header("X-User-Id", userId).header("Accept", "text/event-stream").build();
        openStreams.add(http.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> response.body().forEach(lines::add)));
        assertThat(nextMatching(lines, "event:connected", Duration.ofSeconds(10)))
                .as("stream for %s should confirm the connection", userId).isNotNull();
        return lines;
    }

    private static String nextMatching(BlockingQueue<String> lines, String fragment, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            String line = lines.poll(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            if (line != null && line.contains(fragment)) {
                return line;
            }
        }
        return null;
    }

    @Test
    void eachEventReachesOnlyItsOwner() throws Exception {
        BlockingQueue<String> userA = openStreamAs("user_a");
        BlockingQueue<String> userB = openStreamAs("user_b");

        kafka.send(KafkaConfig.TRANSACTION_EVENTS_TOPIC, """
                {"transactionId":"11111111-2222-3333-4444-555555555555","userId":"user_a",
                 "upiId":"shop@oksbi","amount":250.00,"currency":"INR","type":"UPI"}""");
        kafka.send(KafkaConfig.BILL_REMINDERS_TOPIC, """
                {"userId":"user_b","type":"CC_BILL","payeeName":"HDFC Credit Card","amount":1200,
                 "dueDate":"2026-12-01","daysUntilDue":2}""");

        // Generous first timeout: the consumer group has to join before anything is delivered.
        assertThat(nextMatching(userA, "Payment of ₹250.00 to shop@oksbi", Duration.ofSeconds(30))).isNotNull();
        assertThat(nextMatching(userB, "HDFC Credit Card of ₹1200 is due in 2 days", Duration.ofSeconds(30))).isNotNull();

        // Both events have been processed by now; neither user may see the other's.
        assertThat(nextMatching(userA, "HDFC", Duration.ofSeconds(2))).isNull();
        assertThat(nextMatching(userB, "shop@oksbi", Duration.ofSeconds(2))).isNull();
    }

    @Test
    void streamWithoutUserIsRejected() throws Exception {
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/notifications/stream"))
                        .header("Accept", "text/event-stream").build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
    }
}
