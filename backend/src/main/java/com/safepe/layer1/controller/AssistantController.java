package com.safepe.layer1.controller;

import com.safepe.service.GeminiAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * General AI Assistant — Gemini-powered chatbot.
 * ==============================================
 * Separate from the fraud engine. Answers general user questions
 * (payments help, budgeting, how-to, general Q&A) via Gemini.
 *
 * Endpoint: POST /api/v1/assistant/chat
 * Request body:  { "message": "..." }
 * Response body: { "reply": "..." }
 */
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
