package com.safepe.gateway.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Externalised rate-limit tiers. Tuning limits must not require a rebuild,
 * and the expensive Gemini-backed routes need far tighter budgets than
 * read-only history lookups.
 */
@Component
@ConfigurationProperties(prefix = "safepe.rate-limit")
@Data
public class RateLimitProperties {

    /** Master switch — set false to disable limiting entirely (e.g. load tests). */
    private boolean enabled = true;

    /** Buckets held in memory before an idle sweep is triggered. */
    private int maxBuckets = 50_000;

    /** Paths that bypass limiting completely (prefix match). */
    private List<String> exemptPaths = new ArrayList<>();

    /** Applied when no tier prefix matches. */
    private Tier defaultTier = new Tier();

    /** Longest matching prefix wins. */
    private List<Tier> tiers = new ArrayList<>();

    @Data
    public static class Tier {
        /** Path prefix this tier applies to. Ignored on {@code defaultTier}. */
        private String pathPrefix = "";
        /** Maximum burst — tokens available when the bucket is full. */
        private long capacity = 40;
        /** Tokens added back each refill period. */
        private long refillTokens = 20;
        /** Length of one refill period, in seconds. */
        private long refillPeriodSeconds = 1;
    }
}
