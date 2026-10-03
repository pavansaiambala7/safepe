package com.safepe.notification.controller;

import com.safepe.notification.service.NotificationSSEService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationSSEController {

    private final NotificationSSEService notificationSSEService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribe(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        // Set by the gateway after verifying the JWT; the gateway route is not
        // public, so a missing header means the request bypassed it.
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("🔔 New SSE notification stream subscription request for user {}", userId);
        return ResponseEntity.ok(notificationSSEService.subscribe(userId));
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "active_subscribers", notificationSSEService.getActiveSubscriberCount(),
                "status", "active",
                "stream_url", "/api/v1/notifications/stream"
        );
    }
}
