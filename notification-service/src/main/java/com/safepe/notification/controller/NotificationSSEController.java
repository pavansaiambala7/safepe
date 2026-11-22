package com.safepe.notification.controller;

import com.safepe.notification.service.NotificationSSEService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class NotificationSSEController {

    private final NotificationSSEService notificationSSEService;

    /**
     * Per-user notification stream. This path is protected at the gateway, which
     * verifies the Clerk JWT and sets X-User-Id; the service is not reachable
     * from outside the Docker network, so the header cannot be forged.
     */
    @GetMapping(value = "/api/v1/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribe(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null || userId.isBlank()) {
            // Bodiless on purpose: the client asked for text/event-stream, so a JSON
            // error body cannot be written and would turn this 401 into a 500.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(notificationSSEService.subscribe(userId));
    }

    @GetMapping("/api/v1/public/notifications/status")
    public Map<String, Object> status() {
        return Map.of(
                "status", "active",
                "active_connections", notificationSSEService.getActiveSubscriberCount()
        );
    }
}
