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
import java.util.concurrent.CopyOnWriteArrayList;

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

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30 min timeout

        emitters.add(emitter);
        log.info("🔔 New SSE subscriber connected. Total active: {}", emitters.size());

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"status\":\"connected\",\"message\":\"SafePe notification stream active\"}"));
        } catch (IOException e) {
            log.warn("Failed to send SSE connection confirmation");
        }

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

    public void broadcast(NotificationEvent event) {
        if (emitters.isEmpty()) {
            log.debug("No SSE subscribers — skipping broadcast for: {}", event.getType());
            return;
        }

        try {
            String jsonPayload = objectMapper.writeValueAsString(event);

            log.info("📢 Broadcasting {} notification to {} subscriber(s): {}",
                    event.getType(), emitters.size(), event.getTitle());

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
                emitters.removeAll(deadEmitters);
                log.debug("Cleaned up {} dead SSE emitters", deadEmitters.size());
            }

        } catch (Exception e) {
            log.error("❌ Failed to broadcast SSE notification: {}", e.getMessage());
        }
    }

    public int getActiveSubscriberCount() {
        return emitters.size();
    }
}
