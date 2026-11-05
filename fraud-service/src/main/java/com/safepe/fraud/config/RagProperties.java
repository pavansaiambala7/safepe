package com.safepe.fraud.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds the safepe.rag.* block in application.yml.
 * <p>
 * These keys already existed in the YAML but nothing read them - the values
 * were hardcoded as private static final constants in VectorSearchService, so
 * editing the config appeared to work and silently did nothing.
 * <p>
 * similarityThreshold is tuned to the embedding model: 0.75 for
 * gemini-embedding-001 (see application.yml for the measurements). Changing
 * the model without re-tuning breaks the scam / legitimate separation, so
 * re-run rag-eval/ whenever it changes.
 */
@Component
@ConfigurationProperties(prefix = "safepe.rag")
@Data
public class RagProperties {

    /** How long a search result set stays in Redis. */
    private long cacheTtlSeconds = 300;

    /** Minimum cosine similarity (0.0-1.0) for a pattern to count as a match. */
    private double similarityThreshold = 0.75;

    /** Maximum patterns returned from a single search. */
    private int maxResults = 5;
}
