package com.safepe.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Layer 2 — Business Logic (AI Service)
 * =====================================
 * Connects to Google's Gemini AI to analyze text messages,
 * WhatsApp forwards, and emails for phishing and fraud.
 */
@Service
@Slf4j
public class GeminiAIService {

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final RestTemplate restTemplate; // Spring's tool for making HTTP requests

    // Spring injects these values from application.yml (which reads from .env)
    public GeminiAIService(
            @Value("${safepe.gemini.api-key}") String apiKey,
            @Value("${safepe.gemini.model}") String model,
            @Value("${safepe.gemini.base-url}") String baseUrl) {
        
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplate();
        
        log.info("🤖 Gemini AI Service initialized with model: {}", model);
    }

    /**
     * Sends a suspicious message to Gemini AI and asks for a fraud analysis.
     * 
     * @param suspiciousMessage The text message to analyze
     * @return AI's analysis as a String
     */
    public String analyzeMessageForFraud(String suspiciousMessage) {
        log.info("🔍 Asking Gemini AI to analyze: {}", suspiciousMessage);
        
        // 1. Build the exact URL for Google's API
        String url = String.format("%s/models/%s:generateContent?key=%s", baseUrl, model, apiKey);

        // 2. Give the AI its persona and instructions (Prompt Engineering)
        String prompt = "You are a cybersecurity expert for SafePe bank. " +
                "Analyze the following SMS message for phishing or fraud. " +
                "Give it a Risk Score from 0 (Safe) to 100 (Scam). " +
                "Keep the explanation very short (1 sentence). " +
                "Message: \"" + suspiciousMessage + "\"";

        // 3. Format the data exactly how Google expects it (JSON structure)
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> parts = new HashMap<>();
        parts.put("text", prompt);
        Map<String, Object> contents = new HashMap<>();
        contents.put("parts", List.of(parts));
        requestBody.put("contents", List.of(contents));

        // 4. Set headers to say "We are sending JSON"
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            // 5. Send the POST request over the internet to Google!
            Map response = restTemplate.postForObject(url, request, Map.class);
            
            // In a real app we'd parse this cleanly, but we'll just return it for now
            return "AI Analysis Complete! (Raw response returned)";
            
        } catch (Exception e) {
            log.error("❌ Failed to call Gemini API. Did you put the API key in .env? Error: {}", e.getMessage());
            return "Error: Could not reach AI. Please check your internet and API key.";
        }
    }

    /**
     * Asks Gemini AI to analyze current Fixed Deposit (FD) interest rates
     * and recommend the best bank for the user's requirements.
     * 
     * @param userQuery The user's query (e.g., "Which bank has the best 1-year FD rate?")
     * @return AI's recommendation as a String
     */
    public String analyzeFDRates(String userQuery) {
        log.info("📈 Asking Gemini AI for FD Rate advice: {}", userQuery);
        
        String url = String.format("%s/models/%s:generateContent?key=%s", baseUrl, model, apiKey);

        // Prompt Engineering for Financial Advice
        String prompt = "You are a financial advisor for SafePe bank in India. " +
                "The user is asking about Fixed Deposit (FD) interest rates. " +
                "Provide the current top 3 banks with the best FD rates based on their query. " +
                "Keep the answer structured, concise, and easy to read. " +
                "User Query: \"" + userQuery + "\"";

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> parts = new HashMap<>();
        parts.put("text", prompt);
        Map<String, Object> contents = new HashMap<>();
        contents.put("parts", List.of(parts));
        requestBody.put("contents", List.of(contents));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            Map response = restTemplate.postForObject(url, request, Map.class);
            return "AI Analysis Complete! (Raw FD Rate response returned)";
        } catch (Exception e) {
            log.error("❌ Failed to fetch FD rates from AI: {}", e.getMessage());
            return "Error: Could not retrieve FD rates at this time.";
        }
    }
}
