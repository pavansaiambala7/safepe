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
    private String cachedFDRates = null;

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
     * Automatically fetch FD rates when the backend starts up
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("🚀 Application started! Automatically pre-fetching FD Rates in the background...");
        new Thread(() -> {
            try {
                this.cachedFDRates = fetchRatesFromGemini();
                log.info("✅ Successfully pre-fetched and cached FD rates from AI!");
            } catch (Exception e) {
                log.error("❌ Failed to pre-fetch FD rates: {}", e.getMessage());
            }
        }).start();
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
            
            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    if (content != null && content.containsKey("parts")) {
                        List<Map<String, Object>> partsList = (List<Map<String, Object>>) content.get("parts");
                        if (!partsList.isEmpty()) {
                            return (String) partsList.get(0).get("text");
                        }
                    }
                }
            }
            return "Could not parse AI response. Please try again.";
            
        } catch (Exception e) {
            log.error("❌ Failed to call Gemini API. Did you put the API key in .env? Error: {}", e.getMessage());
            return "Error: Could not reach AI. Please check your internet and API key.";
        }
    }

    /**
     * Returns the cached FD rates instantly to the user.
     */
    public String analyzeFDRates(String userQuery) {
        if (cachedFDRates != null) {
            log.info("⚡ Returning instantly cached FD rates!");
            return cachedFDRates;
        }
        log.info("⚠️ Cache miss! Fetching FD rates directly from Gemini...");
        return fetchRatesFromGemini();
    }

    private String fetchRatesFromGemini() {
        String url = String.format("%s/models/%s:generateContent?key=%s", baseUrl, model, apiKey);

        // Prompt Engineering - Replaced Jio with Axis Bank and updated Kotak name
        String prompt = "You are a financial advisor for SafePe bank. Provide the current best Fixed Deposit (FD) interest rates for both General and Senior Citizens for exactly these 10 banks: HDFC, SBI, ICICI, Canara, Kotak Mahindra Bank, Yes Bank, Axis Bank, Airtel Payments Bank, Union Bank, Federal Bank. " +
                "Order the banks by the highest general citizen rate descending. " +
                "IMPORTANT: Return the response ONLY as a valid JSON array of objects. Do not include markdown like ```json, just raw JSON. " +
                "Each object MUST have keys: 'bank' (string), 'domain' (string, e.g., 'hdfcbank.com', 'sbi.co.in'), 'normal' (string, e.g., '7.10%'), and 'senior' (string, e.g., '7.60%').";

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
            
            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    if (content != null && content.containsKey("parts")) {
                        List<Map<String, Object>> partsList = (List<Map<String, Object>>) content.get("parts");
                        if (!partsList.isEmpty()) {
                            return (String) partsList.get(0).get("text");
                        }
                    }
                }
            }
            return "Could not parse AI response. Please try again.";
        } catch (Exception e) {
            log.error("❌ Failed to fetch FD rates from AI: {}", e.getMessage());
            return "Error: Could not retrieve FD rates at this time.";
        }
    }
}
