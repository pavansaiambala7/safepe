package com.safepe.fraud.service.rag;

import com.safepe.fraud.config.RagProperties;
import com.safepe.fraud.model.FraudPattern;
import com.safepe.fraud.repository.FraudPatternRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
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

    private final RagProperties ragProperties;

    private List<PatternWithEmbedding> patternEmbeddingCache = null;

    public VectorSearchService(
            GeminiEmbeddingService embeddingService,
            FraudPatternRepository fraudPatternRepository,
            RedisTemplate<String, Object> redisTemplate,
            RagProperties ragProperties) {
        this.embeddingService = embeddingService;
        this.fraudPatternRepository = fraudPatternRepository;
        this.redisTemplate = redisTemplate;
        this.ragProperties = ragProperties;
    }

    public List<VectorSearchResult> searchSimilarPatterns(String message) {
        long startTime = System.currentTimeMillis();
        String cacheKey = CACHE_PREFIX + cacheKeyFor(message);

        List<VectorSearchResult> cached = readFromCache(cacheKey);
        if (cached != null) {
            log.info("Redis cache HIT for vector search ({} ms)",
                    System.currentTimeMillis() - startTime);
            return cached;
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
                if (similarity >= ragProperties.getSimilarityThreshold()) {
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
                .limit(ragProperties.getMaxResults())
                .collect(Collectors.toList());

        try {
            redisTemplate.opsForValue().set(cacheKey, results,
                    ragProperties.getCacheTtlSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("Could not cache vector search results: {}", e.getMessage());
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("🔍 Vector search completed in {} ms — {} matches above threshold", elapsed, results.size());

        return results;
    }

    /**
     * Reads the cache without an unchecked cast.
     * <p>
     * The previous version did {@code return (List<VectorSearchResult>) cached;}
     * which, because generics are erased, never threw at the cast site. A cache
     * entry deserialized as List<LinkedHashMap> therefore escaped this method
     * and blew up with ClassCastException in the CALLER, outside the try/catch,
     * turning one poisoned key into a permanent 500 for that query. Validating
     * element types here keeps the failure local and self-healing.
     *
     * @return the cached results, or null on a miss or an unusable entry
     */
    private List<VectorSearchResult> readFromCache(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (!(cached instanceof List<?> list)) {
                return null;
            }
            List<VectorSearchResult> typed = new ArrayList<>(list.size());
            for (Object element : list) {
                if (!(element instanceof VectorSearchResult result)) {
                    log.warn("Discarding cache entry {} - unexpected element type {}",
                            cacheKey, element == null ? "null" : element.getClass().getName());
                    redisTemplate.delete(cacheKey);
                    return null;
                }
                typed.add(result);
            }
            return typed;
        } catch (Exception e) {
            log.debug("Redis cache unavailable, proceeding without cache: {}", e.getMessage());
            return null;
        }
    }

    /**
     * SHA-256 rather than String.hashCode(): hashCode collides readily (the
     * classic example being "FB" and "Ea"), and a collision here would serve
     * one query the fraud patterns matched for a completely different message.
     */
    private String cacheKeyFor(String message) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 16);
        } catch (Exception e) {
            return Integer.toHexString(message.hashCode());
        }
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
                .limit(ragProperties.getMaxResults())
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
