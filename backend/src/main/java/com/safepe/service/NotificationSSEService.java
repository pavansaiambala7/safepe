package com.safepe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safepe.dto.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Notification SSE Service
 * =========================
 * Manages Server-Sent Event emitters for real-time notification
 * broadcasting from Kafka consumers to React frontend bell icon.
 *
 * Architecture:
 *   Kafka Consumer → NotificationSSEService.broadcast() → SseEmitter → React EventSource → Bell 🔔
 */
@Service
@Slf4j
public class NotificationSSEService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;

    public NotificationSSEService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Registers a new SSE emitter for a client connection.
     * Sets a 30-minute timeout and auto-cleanup on completion/timeout/error.
     */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30 min timeout

        emitters.add(emitter);
        log.info("🔔 New SSE subscriber connected. Total active: {}", emitters.size());

        // Send initial heartbeat to confirm connection
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"status\":\"connected\",\"message\":\"SafePe notification stream active\"}"));
        } catch (IOException e) {
            log.warn("Failed to send SSE connection confirmation");
        }

        // Cleanup on disconnect
        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.info("📡 SSE subscriber disconnected (completion). Active: {}", emitters.size());
        });
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.info("📡 SSE subscriber disconnected (timeout). Active: {}", emitters.size());
        });
        emitter.onError(e -> {
            emitters.remove(emitter);
            log.debug("📡 SSE subscriber disconnected (error). Active: {}", emitters.size());
        });

        return emitter;
    }

    /**
     * Broadcasts a notification event to ALL connected SSE clients.
     * Dead emitters are automatically cleaned up on send failure.
     */
    public void broadcast(NotificationEvent event) {
        if (emitters.isEmpty()) {
            log.debug("No SSE subscribers — skipping broadcast for: {}", event.getType());
            return;
        }

        try {
            String jsonPayload = objectMapper.writeValueAsString(event);

            log.info("📢 Broadcasting {} notification to {} subscriber(s): {}",
                    event.getType(), emitters.size(), event.getTitle());

            List<SseEmitter> deadEmitters = new java.util.ArrayList<>();

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("notification")
                            .data(jsonPayload));
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                }
            }

            // Clean up dead connections
            if (!deadEmitters.isEmpty()) {
                emitters.removeAll(deadEmitters);
                log.debug("Cleaned up {} dead SSE emitters", deadEmitters.size());
            }

        } catch (Exception e) {
            log.error("❌ Failed to broadcast SSE notification: {}", e.getMessage());
        }
    }

    /**
     * Returns the count of active SSE subscribers.
     */
    public int getActiveSubscriberCount() {
        return emitters.size();
    }
}
