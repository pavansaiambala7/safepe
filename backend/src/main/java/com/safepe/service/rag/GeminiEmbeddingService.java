package com.safepe.service.rag;

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
 * Gemini Embedding Service — RAG Pipeline
 * =========================================
 * Generates 768-dimension vector embeddings using Google's
 * Gemini text-embedding-004 model. These embeddings are used for:
 *   1. Indexing fraud patterns into pgvector
 *   2. Converting user queries into vectors for similarity search
 *
 * Performance: ~200ms per embedding request to Gemini API
 */
@Service
@Slf4j
public class GeminiEmbeddingService {

    private final String apiKey;
    private final String embeddingModel;
    private final String baseUrl;
    private final RestTemplate restTemplate;

    public GeminiEmbeddingService(
            @Value("${safepe.gemini.api-key}") String apiKey,
            @Value("${safepe.gemini.embedding-model:text-embedding-004}") String embeddingModel,
            @Value("${safepe.gemini.base-url}") String baseUrl) {
        this.apiKey = apiKey;
        this.embeddingModel = embeddingModel;
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplate();
        log.info("🧠 Gemini Embedding Service initialized with model: {}", embeddingModel);
    }

    /**
     * Generates a 768-dimension embedding vector for the given text.
     *
     * @param text The text to embed (fraud pattern description, user message, etc.)
     * @return float array of 768 dimensions, or null on failure
     */
    public float[] generateEmbedding(String text) {
        String url = String.format("%s/models/%s:embedContent?key=%s", baseUrl, embeddingModel, apiKey);

        // Build the request body per Gemini Embedding API spec
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> parts = new HashMap<>();
        parts.put("text", text);
        content.put("parts", List.of(parts));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "models/" + embeddingModel);
        requestBody.put("content", content);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            Map response = restTemplate.postForObject(url, request, Map.class);

            if (response != null && response.containsKey("embedding")) {
                Map<String, Object> embedding = (Map<String, Object>) response.get("embedding");
                List<Number> values = (List<Number>) embedding.get("values");

                if (values != null && !values.isEmpty()) {
                    float[] vector = new float[values.size()];
                    for (int i = 0; i < values.size(); i++) {
                        vector[i] = values.get(i).floatValue();
                    }
                    log.debug("✅ Generated embedding: {} dimensions for text: '{}'",
                            vector.length, text.substring(0, Math.min(50, text.length())));
                    return vector;
                }
            }
            log.warn("⚠️ Empty embedding response from Gemini API");
            return null;

        } catch (Exception e) {
            log.error("❌ Failed to generate embedding from Gemini: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Calculates cosine similarity between two embedding vectors.
     *
     * @return similarity score between -1.0 and 1.0 (higher = more similar)
     */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
