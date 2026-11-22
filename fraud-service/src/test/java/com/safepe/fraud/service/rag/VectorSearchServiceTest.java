package com.safepe.fraud.service.rag;

import com.safepe.fraud.config.RagProperties;
import com.safepe.fraud.model.FraudPattern;
import com.safepe.fraud.repository.FraudPatternRepository;
import com.safepe.fraud.service.rag.VectorSearchService.VectorSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Retrieval step of the fraud RAG. Embeddings are hand-made 2-D vectors, so the
 * cosine similarity of each pattern to the query is known exactly:
 * angle 0 -> 1.00, angle ~25.8 deg -> 0.90, ~41.4 deg -> 0.75, 60 deg -> 0.50.
 */
class VectorSearchServiceTest {

    private static final float[] QUERY = {1f, 0f};

    private final GeminiEmbeddingService embeddings = mock(GeminiEmbeddingService.class);
    private final FraudPatternRepository patterns = mock(FraudPatternRepository.class);
    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, Object> redisValues = mock(ValueOperations.class);
    private final RagProperties props = new RagProperties();
    private VectorSearchService service;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(redisValues);
        service = new VectorSearchService(embeddings, patterns, redis, props);
        when(embeddings.generateEmbedding("query")).thenReturn(QUERY);
    }

    /** Registers patterns whose embedding has the given cosine similarity to QUERY. */
    private void patternsWithSimilarity(Map<String, Double> similarityByDescription) {
        List<FraudPattern> list = new ArrayList<>();
        similarityByDescription.forEach((desc, cos) -> {
            list.add(FraudPattern.builder().id(UUID.randomUUID()).patternDescription(desc)
                    .category("CAT_" + desc).severity("HIGH").build());
            float sin = (float) Math.sqrt(1 - cos * cos);
            when(embeddings.generateEmbedding(desc)).thenReturn(new float[]{cos.floatValue(), sin});
        });
        when(patterns.findAll()).thenReturn(list);
    }

    @Test
    void keepsOnlyPatternsAtOrAboveThresholdSortedBestFirst() {
        patternsWithSimilarity(Map.of("exact", 1.0, "close", 0.90, "edge", 0.7501, "far", 0.50));

        List<VectorSearchResult> results = service.searchSimilarPatterns("query");

        assertThat(results).extracting(VectorSearchResult::getPatternDescription)
                .containsExactly("exact", "close", "edge");
        assertThat(results.get(1).getSimilarityScore()).isEqualTo(90.0);
    }

    @Test
    void returnsAtMostMaxResults() {
        props.setMaxResults(2);
        patternsWithSimilarity(Map.of("a", 0.99, "b", 0.95, "c", 0.90, "d", 0.85));

        assertThat(service.searchSimilarPatterns("query")).extracting(VectorSearchResult::getPatternDescription)
                .containsExactly("a", "b");
    }

    @Test
    void cachesResultsInRedisWithConfiguredTtl() {
        patternsWithSimilarity(Map.of("close", 0.90));

        service.searchSimilarPatterns("query");

        verify(redisValues).set(startsWith("safepe:rag:"), any(), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    void cacheHitSkipsEmbeddingAndPatternLookup() {
        List<VectorSearchResult> cached = List.of(VectorSearchResult.builder().patternDescription("cached").build());
        when(redisValues.get(anyString())).thenReturn(cached);

        assertThat(service.searchSimilarPatterns("query")).extracting(VectorSearchResult::getPatternDescription)
                .containsExactly("cached");
        verifyNoInteractions(embeddings, patterns);
    }

    @Test
    void poisonedCacheEntryIsDeletedAndRecomputed() {
        when(redisValues.get(anyString())).thenReturn(List.of(Map.of("patternDescription", "stale")));
        patternsWithSimilarity(Map.of("close", 0.90));

        List<VectorSearchResult> results = service.searchSimilarPatterns("query");

        verify(redis).delete(startsWith("safepe:rag:"));
        assertThat(results).extracting(VectorSearchResult::getPatternDescription).containsExactly("close");
    }

    @Test
    void redisOutageDoesNotBreakSearch() {
        when(redisValues.get(anyString())).thenThrow(new RuntimeException("connection refused"));
        doThrow(new RuntimeException("connection refused")).when(redisValues).set(anyString(), any(), anyLong(), any());
        patternsWithSimilarity(Map.of("close", 0.90));

        assertThat(service.searchSimilarPatterns("query")).hasSize(1);
    }

    @Test
    void patternEmbeddingsAreComputedOnlyOnce() {
        patternsWithSimilarity(Map.of("close", 0.90));
        when(embeddings.generateEmbedding("query 2")).thenReturn(QUERY);

        service.searchSimilarPatterns("query");
        service.searchSimilarPatterns("query 2");

        verify(embeddings, times(1)).generateEmbedding("close");
        verify(patterns, times(1)).findAll();
    }
}
