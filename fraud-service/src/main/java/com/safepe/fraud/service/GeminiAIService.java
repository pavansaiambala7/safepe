package com.safepe.fraud.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class GeminiAIService {

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final RestTemplate restTemplate;
    private String cachedFDRates = null;

    private final ConcurrentHashMap<Integer, CachedResponse> responseCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

    private static class CachedResponse {
        final String response;
        final long timestamp;
        CachedResponse(String response) {
            this.response = response;
            this.timestamp = System.currentTimeMillis();
        }
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    public GeminiAIService(
            @Value("${safepe.gemini.api-key:dummy_gemini_key}") String apiKey,
            @Value("${safepe.gemini.model:gemini-2.5-flash}") String model,
            @Value("${safepe.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl) {

        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(30_000);
        this.restTemplate = new RestTemplate(factory);

        log.info("🤖 Gemini AI Service initialized with model: {}", model);
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("🚀 Pre-fetching FD Rates in background...");
        new Thread(() -> {
            try {
                this.cachedFDRates = fetchRatesFromGemini();
                log.info("✅ Pre-fetched and cached FD rates from AI");
            } catch (Exception e) {
                log.warn("FD rates prefetch deferred: {}", e.getMessage());
            }
        }).start();
    }

    public String analyzeMessageForFraud(String suspiciousMessage) {
        int cacheKey = suspiciousMessage.hashCode();
        CachedResponse cached = responseCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.info("⚡ Cache HIT for fraud analysis");
            return cached.response;
        }

        log.info("🔍 Asking Gemini AI to analyze message...");
        long startTime = System.currentTimeMillis();

        String url = String.format("%s/models/%s:generateContent?key=%s", baseUrl, model, apiKey);

        String prompt = "You are a cybersecurity expert for SafePe bank. " +
                "Analyze the following SMS message for phishing or fraud. " +
                "Give it a Risk Score from 0 (Safe) to 100 (Scam). " +
                "Keep the explanation very short (1 sentence). " +
                "Message: \"" + suspiciousMessage + "\"";

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
                            String result = (String) partsList.get(0).get("text");
                            long elapsed = System.currentTimeMillis() - startTime;
                            log.info("✅ Gemini response received in {}ms", elapsed);
                            responseCache.put(cacheKey, new CachedResponse(result));
                            return result;
                        }
                    }
                }
            }
            return "Could not parse AI response. Please try again.";

        } catch (Exception e) {
            log.error("❌ Failed to call Gemini API: {}", e.getMessage());
            return "Error: Could not reach AI. Please check your internet and API key.";
        }
    }

    public String chatWithAssistant(String userMessage) {
        int cacheKey = ("chat::" + userMessage).hashCode();
        CachedResponse cached = responseCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.info("⚡ Cache HIT for assistant chat");
            return cached.response;
        }

        log.info("💬 Assistant chat: {}", userMessage.substring(0, Math.min(80, userMessage.length())));
        long startTime = System.currentTimeMillis();

        String url = String.format("%s/models/%s:generateContent?key=%s", baseUrl, model, apiKey);

        String prompt = "You are SafePe Assistant, a friendly and helpful AI assistant inside the SafePe payments app. " +
                "Answer the user's question clearly and concisely. You can help with general questions, " +
                "payments guidance, budgeting tips, and how to use SafePe features. " +
                "Use a warm, conversational tone and keep answers to the point. " +
                "If the user shares something that looks like a scam, gently warn them.\n\n" +
                "User: " + userMessage;

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
                            String result = (String) partsList.get(0).get("text");
                            long elapsed = System.currentTimeMillis() - startTime;
                            log.info("✅ Assistant response received in {}ms", elapsed);
                            responseCache.put(cacheKey, new CachedResponse(result));
                            return result;
                        }
                    }
                }
            }
            return "Sorry, I couldn't generate a response. Please try again.";
        } catch (Exception e) {
            log.error("❌ Assistant chat failed: {}", e.getMessage());
            return "Error: Could not reach the AI right now. Please try again in a moment.";
        }
    }

    public String analyzeFDRates(String userQuery) {
        if (cachedFDRates != null) {
            log.info("⚡ Returning cached FD rates");
            return cachedFDRates;
        }
        return fetchRatesFromGemini();
    }

    private String fetchRatesFromGemini() {
        String url = String.format("%s/models/%s:generateContent?key=%s", baseUrl, model, apiKey);

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
            log.error("❌ Failed to fetch FD rates: {}", e.getMessage());
            return "Error: Could not retrieve FD rates at this time.";
        }
    }
}
