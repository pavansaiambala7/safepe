package com.safepe.gateway.filter;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safepe.gateway.error.GlobalErrorWebExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Spring Cloud Gateway Global JWT Filter
 * =======================================
 * Validates Clerk Bearer JWT tokens at the API Gateway perimeter.
 * Unauthenticated requests to protected routes are rejected with HTTP 401.
 * Valid requests have their user ID propagated via the X-User-Id header.
 */
@Component
@Slf4j
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwkProvider jwkProvider;
    private final String issuer;
    private final ObjectMapper objectMapper;

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/api/health",
            "/actuator",
            "/api/v1/payments/webhook",
            "/api/v1/fraud/check",
            "/api/v1/fraud/patterns/search",
            "/api/v1/fraud/search"
    );

    /**
     * The browser's EventSource cannot set an Authorization header, so the
     * notification stream alone may carry the JWT as a ?token= query parameter.
     */
    private static final String SSE_STREAM_PATH = "/api/v1/notifications/stream";

    public JwtAuthFilter(
            @Value("${safepe.clerk.jwks-url:https://creative-muskox-36.clerk.accounts.dev/.well-known/jwks.json}") String jwksUrl,
            @Value("${safepe.clerk.issuer:https://creative-muskox-36.clerk.accounts.dev}") String issuer,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.jwkProvider = new JwkProviderBuilder(toURL(jwksUrl))
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .build();
        this.issuer = issuer;
        log.info("🔑 API Gateway JwtAuthFilter initialized with JWKS provider (24h cache)");
    }

    private static URL toURL(String url) {
        try {
            return new URL(url);
        } catch (Exception e) {
            log.warn("Invalid JWKS URL configured: {}, falling back to default dummy URL", url);
            try {
                return new URL("https://creative-muskox-36.clerk.accounts.dev/.well-known/jwks.json");
            } catch (Exception ex) {
                throw new RuntimeException("Cannot parse JWKS URL", ex);
            }
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 1. Allow HTTP OPTIONS (CORS preflight)
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // 2. Allow whitelisted public paths - but strip any client-sent X-User-Id.
        //    Downstream services trust that header as the authenticated identity,
        //    so on routes that skip JWT verification a caller could otherwise
        //    forge it and act as any user.
        if (isPublicPath(path)) {
            ServerHttpRequest stripped = request.mutate()
                    .headers(h -> h.remove("X-User-Id"))
                    .build();
            return chain.filter(exchange.mutate().request(stripped).build());
        }

        // 3. Check for Authorization header (or ?token= on the SSE stream only)
        String token = extractToken(request, path);
        if (token == null) {
            log.warn("🚨 Missing or invalid Authorization header for protected route: {}", path);
            return unauthorizedResponse(exchange, "Authorization token is missing or invalid");
        }

        try {
            DecodedJWT decoded = JWT.decode(token);
            Jwk jwk = jwkProvider.get(decoded.getKeyId());
            Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);

            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(issuer)
                    .build();

            DecodedJWT verified = verifier.verify(token);
            String userId = verified.getSubject();

            if (userId == null || userId.isBlank()) {
                return unauthorizedResponse(exchange, "Token has no subject");
            }

            // Overwrite (not append) so a client-sent X-User-Id cannot ride along.
            ServerHttpRequest mutatedRequest = request.mutate()
                    .headers(h -> h.set("X-User-Id", userId))
                    .build();

            log.debug("✅ Verified token for user: {} on path: {}", userId, path);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.warn("🚨 JWT verification failed for path {}: {}", path, e.getMessage());
            return unauthorizedResponse(exchange, "Invalid or expired token");
        }
    }

    private static String extractToken(ServerHttpRequest request, String path) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (path.equals(SSE_STREAM_PATH)) {
            String queryToken = request.getQueryParams().getFirst("token");
            if (queryToken != null && !queryToken.isBlank()) {
                return queryToken;
            }
        }
        return null;
    }

    private boolean isPublicPath(String path) {
        for (String prefix : PUBLIC_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Emits the same ApiError envelope as every other SafePe error.
     * <p>
     * Previously built with String.format, which produced malformed JSON as soon
     * as the message contained a quote. Jackson escapes correctly.
     */
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        return GlobalErrorWebExceptionHandler.writeError(
                exchange, objectMapper, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    @Override
    public int getOrder() {
        return -100; // High priority in filter chain
    }
}
