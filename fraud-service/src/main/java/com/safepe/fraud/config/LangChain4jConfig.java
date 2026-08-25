package com.safepe.fraud.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jConfig {

    @Bean
    public ChatLanguageModel geminiChatModel(
            @Value("${safepe.gemini.api-key:dummy_gemini_key}") String apiKey,
            @Value("${safepe.gemini.model:gemini-2.5-flash}") String modelName) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.2)
                .build();
    }
}
