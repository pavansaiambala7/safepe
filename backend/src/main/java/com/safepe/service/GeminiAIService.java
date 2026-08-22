package com.safepe.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Layer 2 — Business Logic (AI Service)
 * =====================================
 * Connects to Google's Gemini AI to analyze text messages,
 * WhatsApp forwards, and emails for phishing and fraud.
 *
 * PERF: Configured with connection pooling, timeouts, and
 * in-memory response caching to reduce latency.
 */
@Service
@Slf4j
public class GeminiAIService {

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final RestTemplate restTemplate;
    private String cachedFDRates = null;

    // ── In-memory response cache for SMS fraud analysis ──────────────
    // Key: hash of the message, Value: cached response with timestamp
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

    // Spring injects these values from application.yml (which reads from .env)
    public GeminiAIService(
            @Value("${safepe.gemini.api-key}") String apiKey,
            @Value("${safepe.gemini.model}") String model,
            @Value("${safepe.gemini.base-url}") String baseUrl) {
        
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;

        // PERF: Configure RestTemplate with proper timeouts
        // Default RestTemplate has NO timeouts — a slow Gemini response hangs the thread forever
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);  // 5 seconds to establish connection
        factory.setReadTimeout(30_000);    // 30 seconds to read response (AI can be slow)
        this.restTemplate = new RestTemplate(factory);
        
        log.info("🤖 Gemini AI Service initialized with model: {} (connect=5s, read=30s)", model);
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
     * PERF: Results are cached in-memory for 5 minutes to avoid duplicate API calls.
     * 
     * @param suspiciousMessage The text message to analyze
     * @return AI's analysis as a String
     */
    public String analyzeMessageForFraud(String suspiciousMessage) {
        // ── Check cache first ────────────────────────────────────────
        int cacheKey = suspiciousMessage.hashCode();
        CachedResponse cached = responseCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.info("⚡ Cache HIT for fraud analysis (skipping Gemini API call)");
            return cached.response;
        }

        log.info("🔍 Asking Gemini AI to analyze: {}", suspiciousMessage.substring(0, Math.min(80, suspiciousMessage.length())));
        long startTime = System.currentTimeMillis();
        
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
                            String result = (String) partsList.get(0).get("text");
                            long elapsed = System.currentTimeMillis() - startTime;
                            log.info("✅ Gemini response received in {}ms", elapsed);
                            
                            // Cache the response
                            responseCache.put(cacheKey, new CachedResponse(result));
                            return result;
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
