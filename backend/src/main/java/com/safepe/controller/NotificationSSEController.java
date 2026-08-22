package com.safepe.controller;

import com.safepe.service.NotificationSSEService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Notification SSE Controller
 * ============================
 * Provides a Server-Sent Events (SSE) endpoint for the React frontend
 * to subscribe to real-time notifications from the Kafka pipeline.
 *
 * Endpoint: GET /api/v1/public/notifications/stream
 *
 * The frontend connects via EventSource and receives live notifications:
 *   - Payment success
 *   - Fraud detection alerts (from Kafka fraud-alerts topic)
 *   - Escrow refund initiated
 *   - Refund completed
 *
 * This endpoint is public (no JWT required) so that browser EventSource
 * can connect without custom Authorization headers.
 */
@RestController
@RequestMapping("/api/v1/public/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationSSEController {

    private final NotificationSSEService notificationSSEService;

    /**
     * SSE subscription endpoint.
     * Browser calls: new EventSource('/api/v1/public/notifications/stream')
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        log.info("🔔 New SSE notification stream subscription request");
        return notificationSSEService.subscribe();
    }

    /**
     * Health check for the notification system.
     */
    @GetMapping("/status")
    public java.util.Map<String, Object> status() {
        return java.util.Map.of(
                "active_subscribers", notificationSSEService.getActiveSubscriberCount(),
                "status", "active",
                "stream_url", "/api/v1/public/notifications/stream"
        );
    }
}
