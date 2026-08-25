package com.safepe.gateway.filter;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.nio.charset.StandardCharsets;
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

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/api/health",
            "/actuator",
            "/api/v1/payments/webhook",
            "/api/v1/public",
            "/api/v1/fraud/check",
            "/api/v1/fraud/patterns/search",
            "/api/v1/fraud/search"
    );

    public JwtAuthFilter(
            @Value("${safepe.clerk.jwks-url:https://creative-muskox-36.clerk.accounts.dev/.well-known/jwks.json}") String jwksUrl,
            @Value("${safepe.clerk.issuer:https://creative-muskox-36.clerk.accounts.dev}") String issuer) {
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

        // 2. Allow whitelisted public paths
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // 3. Check for Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("🚨 Missing or invalid Authorization header for protected route: {}", path);
            return unauthorizedResponse(exchange, "Authorization token is missing or invalid");
        }

        String token = authHeader.substring(7);

        try {
            DecodedJWT decoded = JWT.decode(token);
            Jwk jwk = jwkProvider.get(decoded.getKeyId());
            Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);

            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(issuer)
                    .build();

            DecodedJWT verified = verifier.verify(token);
            String userId = verified.getSubject();

            // Mutate request to attach downstream X-User-Id header
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId != null ? userId : "anonymous")
                    .build();

            log.debug("✅ Verified token for user: {} on path: {}", userId, path);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.warn("🚨 JWT verification failed for path {}: {}", path, e.getMessage());
            return unauthorizedResponse(exchange, "Invalid or expired token");
        }
    }

    private boolean isPublicPath(String path) {
        for (String prefix : PUBLIC_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] bytes = String.format("{\"error\":\"UNAUTHORIZED\",\"message\":\"%s\"}", message)
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100; // High priority in filter chain
    }
}
