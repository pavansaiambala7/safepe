package com.safepe.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j Configuration — Agentic AI Framework Initialization
 * ===============================================================
 * Initializes the LangChain4j {@link ChatLanguageModel} backed by Google
 * Gemini (via the langchain4j-google-ai-gemini integration module).
 *
 * This bean is injected into the AgenticFraudEngine, so the multi-step
 * fraud-reasoning pipeline runs through the LangChain4j framework
 * abstraction instead of raw HTTP calls. Reuses the same Gemini API key
 * and model name already configured under `safepe.gemini.*`.
 */
@Configuration
public class LangChain4jConfig {

    @Bean
    public ChatLanguageModel geminiChatModel(
            @Value("${safepe.gemini.api-key}") String apiKey,
            @Value("${safepe.gemini.model}") String modelName) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.2)
                .build();
    }
}
