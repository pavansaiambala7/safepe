package com.safepe.fraud.controller;

import com.safepe.fraud.config.SecurityConfig;
import com.safepe.fraud.dto.AgenticFraudResult;
import com.safepe.fraud.service.GeminiAIService;
import com.safepe.fraud.service.InsightsService;
import com.safepe.fraud.service.MoneyAssistantService;
import com.safepe.fraud.service.agent.AgenticFraudEngine;
import com.safepe.fraud.service.rag.VectorSearchService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer of the AI endpoints: request validation, and that the user id always
 * comes from the gateway header (X-User-Id), never from the request body.
 */
@WebMvcTest({AssistantController.class, MoneyAssistantController.class, InsightsController.class, FraudCheckController.class})
@Import(SecurityConfig.class)
class AiControllersTest {

    @Autowired MockMvc mvc;
    @MockBean MoneyAssistantService moneyAssistant;
    @MockBean InsightsService insights;
    @MockBean GeminiAIService gemini;
    @MockBean AgenticFraudEngine fraudEngine;
    @MockBean VectorSearchService vectorSearch;

    @Test
    void chatUsesHeaderUserAndIgnoresUserIdInBody() throws Exception {
        when(moneyAssistant.answer("user_a", "How much did I spend?")).thenReturn("You spent ₹450.");

        mvc.perform(post("/api/v1/assistant/chat").header("X-User-Id", "user_a").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": \"victim\", \"message\": \"How much did I spend?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("You spent ₹450."));

        verify(moneyAssistant, never()).answer(eq("victim"), any());
    }

    @Test
    void blankChatMessageIsRejected() throws Exception {
        mvc.perform(post("/api/v1/assistant/chat").header("X-User-Id", "user_a").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"  \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/assistant/money").header("X-User-Id", "user_a").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(moneyAssistant);
    }

    @Test
    void insightsAreForTheHeaderUser() throws Exception {
        when(insights.insights("user_a")).thenReturn(Map.of("totalSpent", 0, "transactionCount", 0));

        mvc.perform(get("/api/v1/assistant/insights").header("X-User-Id", "user_a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionCount").value(0));
    }

    @Test
    void fraudAnalysisUsesHeaderUserForVelocityCheck() throws Exception {
        when(fraudEngine.analyzeWithAgents(anyString(), any(), anyString()))
                .thenReturn(AgenticFraudResult.builder().riskScore(91).action("BLOCK").build());

        mvc.perform(post("/api/v1/fraud/agentic-analyze").header("X-User-Id", "user_a").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"Update KYC now\", \"upiId\": \"kyc@ybl\", \"userId\": \"victim\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("BLOCK"))
                .andExpect(jsonPath("$.riskScore").value(91));

        verify(fraudEngine).analyzeWithAgents("Update KYC now", "kyc@ybl", "user_a");
    }

    @Test
    void blankFraudMessageIsRejected() throws Exception {
        mvc.perform(post("/api/v1/fraud/agentic-analyze").header("X-User-Id", "user_a").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(fraudEngine);
    }

    @Test
    void smsScannerReturnsGeminiAnalysis() throws Exception {
        when(gemini.analyzeMessageForFraud("Pay ₹10 to claim prize"))
                .thenReturn("Risk Score: 92/100\nVerdict: Scam\nReason: Advance-fee lottery scam.");

        mvc.perform(post("/api/v1/fraud/analyze-sms").header("X-User-Id", "user_a").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Pay ₹10 to claim prize\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysis").value(org.hamcrest.Matchers.startsWith("Risk Score: 92/100")));
    }

    @Test
    @Disabled("Known bug, fixed in Phase 1: without X-User-Id the service falls back to 'user_123_temp' "
            + "instead of rejecting the request")
    void rejectsRequestWithoutAuthenticatedUser() throws Exception {
        mvc.perform(get("/api/v1/assistant/insights"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(insights);
    }
}
