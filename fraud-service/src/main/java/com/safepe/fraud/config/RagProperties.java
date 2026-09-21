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
 * The YAML default for similarityThreshold is 0.60, matching the constant that
 * was actually in effect. The YAML previously declared 0.75; adopting that
 * number here would have quietly tightened matching and dropped results that
 * the deployed system currently returns.
 */
@Component
@ConfigurationProperties(prefix = "safepe.rag")
@Data
public class RagProperties {

    /** How long a search result set stays in Redis. */
    private long cacheTtlSeconds = 300;

    /** Minimum cosine similarity (0.0-1.0) for a pattern to count as a match. */
    private double similarityThreshold = 0.60;

    /** Maximum patterns returned from a single search. */
    private int maxResults = 5;
}
