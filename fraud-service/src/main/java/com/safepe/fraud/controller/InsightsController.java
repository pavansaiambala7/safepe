package com.safepe.fraud.controller;

import com.safepe.fraud.service.InsightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
public class InsightsController {

    private final InsightsService insightsService;

    @GetMapping("/insights")
    public ResponseEntity<?> insights(@RequestParam String userId) {
        return ResponseEntity.ok(insightsService.insights(userId));
    }
}
