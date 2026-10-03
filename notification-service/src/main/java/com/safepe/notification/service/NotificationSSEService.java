package com.safepe.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safepe.notification.dto.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class NotificationSSEService {

    /**
     * Open streams keyed by the gateway-verified user ID. Events used to be
     * broadcast to every connected browser, so any visitor saw every user's
     * payment amounts and UPI IDs.
     */
    private final Map<String, List<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public NotificationSSEService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30 min timeout

        List<SseEmitter> userEmitters =
                emittersByUser.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        userEmitters.add(emitter);
        log.info("🔔 New SSE subscriber connected for user {}. Total active: {}",
                userId, getActiveSubscriberCount());

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"status\":\"connected\",\"message\":\"SafePe notification stream active\"}"));
        } catch (IOException e) {
            log.warn("Failed to send SSE connection confirmation");
        }

        emitter.onCompletion(() -> {
            remove(userId, emitter);
            log.info("📡 SSE subscriber disconnected (completion). Active: {}", getActiveSubscriberCount());
        });
        emitter.onTimeout(() -> {
            remove(userId, emitter);
            log.info("📡 SSE subscriber disconnected (timeout). Active: {}", getActiveSubscriberCount());
        });
        emitter.onError(e -> {
            remove(userId, emitter);
            log.debug("📡 SSE subscriber disconnected (error). Active: {}", getActiveSubscriberCount());
        });

        return emitter;
    }

    private void remove(String userId, SseEmitter emitter) {
        emittersByUser.computeIfPresent(userId, (k, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list;
        });
    }

    /** Sends the event only to the streams opened by {@code userId}. */
    public void sendToUser(String userId, NotificationEvent event) {
        if (userId == null) {
            log.warn("Dropping {} notification with no userId", event.getType());
            return;
        }
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("No SSE subscribers for user {} — skipping {}", userId, event.getType());
            return;
        }

        try {
            String jsonPayload = objectMapper.writeValueAsString(event);

            log.info("📢 Sending {} notification to {} stream(s) of user {}: {}",
                    event.getType(), emitters.size(), userId, event.getTitle());

            List<SseEmitter> deadEmitters = new ArrayList<>();

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("notification")
                            .data(jsonPayload));
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                }
            }

            if (!deadEmitters.isEmpty()) {
                deadEmitters.forEach(dead -> remove(userId, dead));
                log.debug("Cleaned up {} dead SSE emitters", deadEmitters.size());
            }

        } catch (Exception e) {
            log.error("❌ Failed to send SSE notification: {}", e.getMessage());
        }
    }

    public int getActiveSubscriberCount() {
        return emittersByUser.values().stream().mapToInt(List::size).sum();
    }
}
