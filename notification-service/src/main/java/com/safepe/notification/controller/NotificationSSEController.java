package com.safepe.notification.controller;

import com.safepe.notification.service.NotificationSSEService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationSSEController {

    private final NotificationSSEService notificationSSEService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        log.info("🔔 New SSE notification stream subscription request");
        return notificationSSEService.subscribe();
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "active_subscribers", notificationSSEService.getActiveSubscriberCount(),
                "status", "active",
                "stream_url", "/api/v1/public/notifications/stream"
        );
    }
}
