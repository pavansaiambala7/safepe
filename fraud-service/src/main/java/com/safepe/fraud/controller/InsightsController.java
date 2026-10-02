package com.safepe.fraud.controller;

import com.safepe.fraud.service.InsightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
public class InsightsController {

    private final InsightsService insightsService;

    @GetMapping("/insights")
    public ResponseEntity<?> insights(Principal principal) {
        // Previously @RequestParam userId - any caller could request any user's insights.
        return ResponseEntity.ok(insightsService.insights(principal.getName()));
    }
}
