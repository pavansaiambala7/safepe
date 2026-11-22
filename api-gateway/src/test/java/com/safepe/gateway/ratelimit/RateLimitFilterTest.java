package com.safepe.gateway.ratelimit;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private final AtomicInteger passed = new AtomicInteger();
    private final GatewayFilterChain chain = exchange -> {
        passed.incrementAndGet();
        return Mono.empty();
    };
    private RateLimitProperties props;

    @BeforeEach
    void props() {
        props = new RateLimitProperties();
        RateLimitProperties.Tier ai = new RateLimitProperties.Tier();
        ai.setPathPrefix("/api/v1/assistant");
        ai.setCapacity(2);
        ai.setRefillTokens(1);
        ai.setRefillPeriodSeconds(60);
        props.setTiers(List.of(ai));
        props.setExemptPaths(List.of("/api/v1/notifications/stream"));
    }

    private MockServerWebExchange call(RateLimitFilter filter, MockServerHttpRequest.BaseBuilder<?> request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        filter.filter(exchange, chain).block();
        return exchange;
    }

    private RateLimitFilter filter() {
        return new RateLimitFilter(props, JsonMapper.builder().findAndAddModules().build());
    }

    @Test
    void requestOverTheBurstGets429WithRetryAfter() {
        RateLimitFilter filter = filter();
        call(filter, MockServerHttpRequest.get("/api/v1/assistant/chat").header("X-User-Id", "user_a"));
        call(filter, MockServerHttpRequest.get("/api/v1/assistant/chat").header("X-User-Id", "user_a"));

        MockServerWebExchange third = call(filter, MockServerHttpRequest.get("/api/v1/assistant/chat").header("X-User-Id", "user_a"));

        assertThat(passed).hasValue(2);
        assertThat(third.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(third.getResponse().getHeaders().getFirst("Retry-After")).isNotBlank();
        assertThat(third.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(third.getResponse().getBodyAsString().block()).contains("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void eachUserHasTheirOwnBucket() {
        RateLimitFilter filter = filter();
        for (int i = 0; i < 2; i++) {
            call(filter, MockServerHttpRequest.get("/api/v1/assistant/chat").header("X-User-Id", "user_a"));
        }

        MockServerWebExchange otherUser = call(filter, MockServerHttpRequest.get("/api/v1/assistant/chat").header("X-User-Id", "user_b"));

        assertThat(otherUser.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(passed).hasValue(3);
    }

    @Test
    void anonymousCallersAreLimitedByForwardedClientIp() {
        RateLimitFilter filter = filter();
        for (int i = 0; i < 2; i++) {
            call(filter, MockServerHttpRequest.get("/api/v1/assistant/chat").header("X-Forwarded-For", "203.0.113.7, 10.0.0.1"));
        }

        assertThat(call(filter, MockServerHttpRequest.get("/api/v1/assistant/chat").header("X-Forwarded-For", "203.0.113.7"))
                .getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(call(filter, MockServerHttpRequest.get("/api/v1/assistant/chat").header("X-Forwarded-For", "198.51.100.9"))
                .getResponse().getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void pathsOutsideATierUseTheDefaultTier() {
        RateLimitFilter filter = filter();
        for (int i = 0; i < 10; i++) {
            call(filter, MockServerHttpRequest.get("/api/v1/history/transactions").header("X-User-Id", "user_a"));
        }

        assertThat(passed).hasValue(10); // default burst is 40
    }

    @Test
    void notificationStreamAndPreflightAreNeverLimited() {
        RateLimitFilter filter = filter();
        for (int i = 0; i < 5; i++) {
            call(filter, MockServerHttpRequest.get("/api/v1/notifications/stream").header("X-User-Id", "user_a"));
            call(filter, MockServerHttpRequest.options("/api/v1/assistant/chat").header("X-User-Id", "user_a"));
        }

        assertThat(passed).hasValue(10);
    }

    @Test
    void disabledLimiterLetsEverythingThrough() {
        props.setEnabled(false);
        RateLimitFilter filter = filter();
        for (int i = 0; i < 5; i++) {
            call(filter, MockServerHttpRequest.get("/api/v1/assistant/chat").header("X-User-Id", "user_a"));
        }

        assertThat(passed).hasValue(5);
    }
}
