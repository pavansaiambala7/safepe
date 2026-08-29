package com.safepe.fraud.controller;

import com.safepe.fraud.service.GeminiAIService;
import com.safepe.fraud.service.MoneyAssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final GeminiAIService geminiAIService;
    private final MoneyAssistantService moneyAssistantService;

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, String> request, Principal principal) {
        String message = request.getOrDefault("message", "");
        String userId = request.getOrDefault("userId", principal != null ? principal.getName() : null);

        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "message is required"));
        }

        String reply;
        if (userId != null && !userId.isBlank()) {
            reply = moneyAssistantService.answer(userId, message);
        } else {
            reply = geminiAIService.chatWithAssistant(message);
        }

        return ResponseEntity.ok(Map.of("reply", reply));
    }
}
