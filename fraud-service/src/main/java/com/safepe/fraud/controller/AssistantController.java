package com.safepe.fraud.controller;

import com.safepe.fraud.service.GeminiAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final GeminiAIService geminiAIService;

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "");
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "message is required"));
        }
        String reply = geminiAIService.chatWithAssistant(message);
        return ResponseEntity.ok(Map.of("reply", reply));
    }
}
