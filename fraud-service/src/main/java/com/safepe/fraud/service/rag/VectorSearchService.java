package com.safepe.fraud.service.rag;

import com.safepe.fraud.model.FraudPattern;
import com.safepe.fraud.repository.FraudPatternRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VectorSearchService {

    private final GeminiEmbeddingService embeddingService;
    private final FraudPatternRepository fraudPatternRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "safepe:rag:";
    private static final long CACHE_TTL_SECONDS = 300; // 5 minutes
    private static final double SIMILARITY_THRESHOLD = 0.60;
    private static final int MAX_RESULTS = 5;

    private List<PatternWithEmbedding> patternEmbeddingCache = null;

    public VectorSearchService(
            GeminiEmbeddingService embeddingService,
            FraudPatternRepository fraudPatternRepository,
            RedisTemplate<String, Object> redisTemplate) {
        this.embeddingService = embeddingService;
        this.fraudPatternRepository = fraudPatternRepository;
        this.redisTemplate = redisTemplate;
    }

    public List<VectorSearchResult> searchSimilarPatterns(String message) {
        long startTime = System.currentTimeMillis();
        String cacheKey = CACHE_PREFIX + message.hashCode();

        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null && cached instanceof List) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.info("⚡ Redis cache HIT for vector search ({} ms)", elapsed);
                return (List<VectorSearchResult>) cached;
            }
        } catch (Exception e) {
            log.debug("Redis cache unavailable, proceeding without cache: {}", e.getMessage());
        }

        float[] queryEmbedding = embeddingService.generateEmbedding(message);
        if (queryEmbedding == null) {
            log.warn("⚠️ Could not generate embedding for query, falling back to keyword search");
            return fallbackKeywordSearch(message);
        }

        if (patternEmbeddingCache == null) {
            loadPatternEmbeddings();
        }

        List<VectorSearchResult> results = new ArrayList<>();

        for (PatternWithEmbedding pwe : patternEmbeddingCache) {
            if (pwe.embedding != null) {
                double similarity = GeminiEmbeddingService.cosineSimilarity(queryEmbedding, pwe.embedding);
                if (similarity >= SIMILARITY_THRESHOLD) {
                    results.add(VectorSearchResult.builder()
                            .patternId(pwe.pattern.getId().toString())
                            .patternDescription(pwe.pattern.getPatternDescription())
                            .category(pwe.pattern.getCategory())
                            .severity(pwe.pattern.getSeverity())
                            .similarityScore(Math.round(similarity * 10000.0) / 100.0)
                            .build());
                }
            }
        }

        results = results.stream()
                .sorted(Comparator.comparingDouble(VectorSearchResult::getSimilarityScore).reversed())
                .limit(MAX_RESULTS)
                .collect(Collectors.toList());

        try {
            redisTemplate.opsForValue().set(cacheKey, results, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("Could not cache vector search results: {}", e.getMessage());
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("🔍 Vector search completed in {} ms — {} matches above threshold", elapsed, results.size());

        return results;
    }

    private synchronized void loadPatternEmbeddings() {
        if (patternEmbeddingCache != null) return;

        log.info("📦 Loading fraud pattern embeddings into memory...");
        List<FraudPattern> patterns = fraudPatternRepository.findAll();
        patternEmbeddingCache = new ArrayList<>();

        for (FraudPattern pattern : patterns) {
            float[] embedding = embeddingService.generateEmbedding(pattern.getPatternDescription());
            patternEmbeddingCache.add(new PatternWithEmbedding(pattern, embedding));
        }
        log.info("✅ Loaded {} fraud patterns with embeddings", patternEmbeddingCache.size());
    }

    public void invalidateCache() {
        this.patternEmbeddingCache = null;
        log.info("🔄 Pattern embedding cache invalidated");
    }

    private List<VectorSearchResult> fallbackKeywordSearch(String message) {
        String lower = message.toLowerCase();
        List<FraudPattern> allPatterns = fraudPatternRepository.findAll();

        return allPatterns.stream()
                .filter(p -> {
                    String desc = p.getPatternDescription().toLowerCase();
                    String keywords = p.getKeywords() != null ? p.getKeywords().toLowerCase() : "";
                    return desc.contains(lower) || lower.contains(desc.split(" ")[0]) ||
                           keywords.contains(lower.split(" ")[0]);
                })
                .limit(MAX_RESULTS)
                .map(p -> VectorSearchResult.builder()
                        .patternId(p.getId().toString())
                        .patternDescription(p.getPatternDescription())
                        .category(p.getCategory())
                        .severity(p.getSeverity())
                        .similarityScore(50.0)
                        .build())
                .collect(Collectors.toList());
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VectorSearchResult implements java.io.Serializable {
        private String patternId;
        private String patternDescription;
        private String category;
        private String severity;
        private double similarityScore;
    }

    @AllArgsConstructor
    private static class PatternWithEmbedding {
        FraudPattern pattern;
        float[] embedding;
    }
}
