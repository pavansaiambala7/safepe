package com.safepe.gateway.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safepe.gateway.error.GlobalErrorWebExceptionHandler;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bucket4j token-bucket rate limiting at the gateway perimeter.
 * <p>
 * Runs at order -50, i.e. AFTER {@code JwtAuthFilter} (-100), so the
 * {@code X-User-Id} header it injects is available and limits can be applied
 * per authenticated user rather than per IP. Anonymous callers fall back to
 * their client IP.
 * <p>
 * Buckets are held in-process, so limits are per gateway instance. That is
 * correct for the current single-instance deployment; running more than one
 * gateway replica requires swapping the backing store for bucket4j-redis so
 * replicas share state.
 */
@Component
@Slf4j
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final char KEY_SEPARATOR = '|';

    private final RateLimitProperties props;
    private final ObjectMapper objectMapper;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AtomicBoolean sweeping = new AtomicBoolean(false);
    private final List<RateLimitProperties.Tier> sortedTiers;

    public RateLimitFilter(RateLimitProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        // Longest prefix first, so /api/v1/fraud/check beats /api/v1/fraud.
        this.sortedTiers = props.getTiers().stream()
                .sorted(Comparator.comparingInt(
                        (RateLimitProperties.Tier t) -> t.getPathPrefix().length()).reversed())
                .toList();
        log.info("Bucket4j rate limiting {} - {} tier(s), default {} tokens per {}s, burst {}",
                props.isEnabled() ? "ENABLED" : "DISABLED", sortedTiers.size(),
                props.getDefaultTier().getRefillTokens(),
                props.getDefaultTier().getRefillPeriodSeconds(),
                props.getDefaultTier().getCapacity());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!props.isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // CORS preflight carries no payload and must never be throttled - a 429
        // on OPTIONS surfaces in the browser as an opaque CORS failure.
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // The SSE stream is one long-lived connection, not repeated calls.
        // Counting it would sever the notification bell on reconnect storms.
        for (String exempt : props.getExemptPaths()) {
            if (path.startsWith(exempt)) {
                return chain.filter(exchange);
            }
        }

        RateLimitProperties.Tier tier = resolveTier(path);
        String key = tier.getPathPrefix() + KEY_SEPARATOR + resolveClientKey(request);

        sweepIfOversized();
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(tier));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            HttpHeaders ok = exchange.getResponse().getHeaders();
            ok.add("X-RateLimit-Limit", String.valueOf(tier.getCapacity()));
            ok.add("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return chain.filter(exchange);
        }

        long retryAfterSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
        log.warn("Rate limit exceeded for key={} path={} - retry in {}s",
                key, path, retryAfterSeconds);

        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.add(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        headers.add("X-RateLimit-Limit", String.valueOf(tier.getCapacity()));
        headers.add("X-RateLimit-Remaining", "0");

        return GlobalErrorWebExceptionHandler.writeError(
                exchange, objectMapper, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED",
                "Too many requests. Please retry in " + retryAfterSeconds + " second(s).");
    }

    private RateLimitProperties.Tier resolveTier(String path) {
        for (RateLimitProperties.Tier tier : sortedTiers) {
            if (!tier.getPathPrefix().isEmpty() && path.startsWith(tier.getPathPrefix())) {
                return tier;
            }
        }
        return props.getDefaultTier();
    }

    private Bucket newBucket(RateLimitProperties.Tier tier) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(tier.getCapacity())
                .refillGreedy(tier.getRefillTokens(),
                        Duration.ofSeconds(tier.getRefillPeriodSeconds()))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Identity for limiting: the authenticated user when known, else the client IP.
     * <p>
     * Nginx sits in front of the gateway, so the socket address is always the
     * proxy. X-Forwarded-For must be read or every caller would share one bucket.
     */
    private String resolveClientKey(ServerHttpRequest request) {
        String userId = request.getHeaders().getFirst("X-User-Id");
        if (userId != null && !userId.isBlank()) {
            return "u:" + userId;
        }
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Left-most entry is the original client.
            return "ip:" + forwarded.split(",")[0].trim();
        }
        var remote = request.getRemoteAddress();
        return "ip:" + (remote != null && remote.getAddress() != null
                ? remote.getAddress().getHostAddress() : "unknown");
    }

    /**
     * Drops buckets that have refilled to capacity - a full bucket means that
     * caller has gone idle, so nothing is lost by forgetting it. Prevents the
     * map from growing without bound under IP churn.
     */
    private void sweepIfOversized() {
        if (buckets.size() < props.getMaxBuckets()) {
            return;
        }
        if (!sweeping.compareAndSet(false, true)) {
            return;
        }
        try {
            int before = buckets.size();
            buckets.entrySet().removeIf(entry -> {
                int sep = entry.getKey().indexOf(KEY_SEPARATOR);
                String prefix = sep > 0 ? entry.getKey().substring(0, sep) : "";
                RateLimitProperties.Tier tier = resolveTier(prefix);
                return entry.getValue().getAvailableTokens() >= tier.getCapacity();
            });
            log.info("Rate-limit bucket sweep: {} -> {}", before, buckets.size());
        } finally {
            sweeping.set(false);
        }
    }

    @Override
    public int getOrder() {
        return -50; // after JwtAuthFilter (-100), before routing
    }
}
