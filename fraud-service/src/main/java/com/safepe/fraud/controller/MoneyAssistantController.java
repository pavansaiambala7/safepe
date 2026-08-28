package com.safepe.fraud.controller;

import com.safepe.fraud.service.MoneyAssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
public class MoneyAssistantController {

    private final MoneyAssistantService moneyAssistantService;

    @PostMapping("/money")
    public ResponseEntity<?> money(@RequestBody Map<String, String> request) {
        String userId = request.getOrDefault("userId", "");
        String question = request.getOrDefault("question", "");
        if (userId.isBlank() || question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId and question are required"));
        }
        return ResponseEntity.ok(Map.of("answer", moneyAssistantService.answer(userId, question)));
    }
}
