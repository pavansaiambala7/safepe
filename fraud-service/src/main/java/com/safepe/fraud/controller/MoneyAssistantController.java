package com.safepe.fraud.controller;

import com.safepe.fraud.service.MoneyAssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
public class MoneyAssistantController {

    private final MoneyAssistantService moneyAssistantService;

    @PostMapping("/money")
    public ResponseEntity<?> money(@RequestBody Map<String, String> request, Principal principal) {
        String question = request.getOrDefault("question", "");
        if (question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "question is required"));
        }
        // userId from the verified Principal only; a body-supplied userId is ignored.
        return ResponseEntity.ok(Map.of("answer", moneyAssistantService.answer(principal.getName(), question)));
    }
}
