package com.safepe.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safepe.notification.dto.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds one SSE connection per open browser tab, grouped by user.
 * <p>
 * Events are delivered only to the connections of the user they belong to.
 * The previous version kept a single list and broadcast every event to every
 * connected browser, so any user could see other users' payments and bills.
 */
@Service
@Slf4j
public class NotificationSSEService {

    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L; // client reconnects with a fresh token

    private final Map<String, List<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public NotificationSSEService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emittersByUser.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable remove = () -> removeEmitter(userId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"status\":\"connected\"}"));
        } catch (IOException e) {
            removeEmitter(userId, emitter);
        }

        log.info("SSE subscriber connected for user {}. Active connections: {}", userId, getActiveSubscriberCount());
        return emitter;
    }

    /** Sends the event to every open connection of {@code userId}, and to no one else. */
    public void sendToUser(String userId, NotificationEvent event) {
        if (userId == null || userId.isBlank()) {
            log.warn("Dropping {} notification with no userId - it cannot be delivered safely", event.getType());
            return;
        }

        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("User {} has no open connection - skipping {} notification", userId, event.getType());
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("Failed to serialise {} notification: {}", event.getType(), e.getMessage());
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(json));
            } catch (IOException | IllegalStateException e) {
                removeEmitter(userId, emitter);
            }
        }
        log.info("Sent {} notification to user {} ({} connection(s))", event.getType(), userId, emitters.size());
    }

    public int getActiveSubscriberCount() {
        return emittersByUser.values().stream().mapToInt(List::size).sum();
    }

    private void removeEmitter(String userId, SseEmitter emitter) {
        emittersByUser.computeIfPresent(userId, (k, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list;
        });
    }
}
