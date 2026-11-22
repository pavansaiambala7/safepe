package com.safepe.gateway.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the gateway's JWT perimeter against a real signature check: a local
 * JWKS endpoint (JDK HttpServer) publishes a freshly generated RSA key, standing
 * in for Clerk.
 */
class JwtAuthFilterTest {

    private static final String ISSUER = "https://test-issuer.example";
    private static final String KID = "test-key";
    private static HttpServer jwksServer;
    private static KeyPair signingKey;
    private static JwtAuthFilter filter;

    private final AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
    private final GatewayFilterChain chain = exchange -> {
        forwarded.set(exchange);
        return Mono.empty();
    };

    @BeforeAll
    static void startJwksServer() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        signingKey = generator.generateKeyPair();
        RSAPublicKey pub = (RSAPublicKey) signingKey.getPublic();
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        byte[] jwks = ("{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\",\"alg\":\"RS256\",\"kid\":\"" + KID + "\","
                + "\"n\":\"" + b64.encodeToString(unsigned(pub.getModulus().toByteArray())) + "\","
                + "\"e\":\"" + b64.encodeToString(unsigned(pub.getPublicExponent().toByteArray())) + "\"}]}")
                .getBytes(StandardCharsets.UTF_8);

        jwksServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        jwksServer.createContext("/.well-known/jwks.json", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jwks.length);
            exchange.getResponseBody().write(jwks);
            exchange.close();
        });
        jwksServer.start();

        String jwksUrl = "http://127.0.0.1:" + jwksServer.getAddress().getPort() + "/.well-known/jwks.json";
        filter = new JwtAuthFilter(jwksUrl, ISSUER, JsonMapper.builder().findAndAddModules().build());
    }

    @AfterAll
    static void stopJwksServer() {
        jwksServer.stop(0);
    }

    /** BigInteger.toByteArray() may prepend a sign byte, which JWK encoding must not include. */
    private static byte[] unsigned(byte[] bytes) {
        return bytes[0] == 0 ? java.util.Arrays.copyOfRange(bytes, 1, bytes.length) : bytes;
    }

    private static String token(String subject, String issuer, Instant expiresAt) {
        return JWT.create().withKeyId(KID).withSubject(subject).withIssuer(issuer).withExpiresAt(expiresAt)
                .sign(Algorithm.RSA256((RSAPublicKey) signingKey.getPublic(), (RSAPrivateKey) signingKey.getPrivate()));
    }

    private MockServerWebExchange run(MockServerHttpRequest.BaseBuilder<?> request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        filter.filter(exchange, chain).block();
        return exchange;
    }

    @Test
    void validTokenForwardsTheVerifiedUserId() {
        run(MockServerHttpRequest.get("/api/v1/assistant/insights")
                .header("Authorization", "Bearer " + token("user_a", ISSUER, Instant.now().plusSeconds(60))));

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("user_a");
    }

    @Test
    void forgedUserHeaderIsReplacedByTheTokenSubject() {
        run(MockServerHttpRequest.get("/api/v1/history/transactions")
                .header("X-User-Id", "victim")
                .header("Authorization", "Bearer " + token("user_a", ISSUER, Instant.now().plusSeconds(60))));

        assertThat(forwarded.get().getRequest().getHeaders().get("X-User-Id")).containsExactly("user_a");
    }

    @Test
    void missingTokenIsRejected() {
        MockServerWebExchange exchange = run(MockServerHttpRequest.get("/api/v1/payments/create").header("X-User-Id", "user_a"));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(forwarded.get()).isNull();
    }

    @Test
    void expiredTokenIsRejected() {
        MockServerWebExchange exchange = run(MockServerHttpRequest.get("/api/v1/payments/create")
                .header("Authorization", "Bearer " + token("user_a", ISSUER, Instant.now().minusSeconds(60))));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(forwarded.get()).isNull();
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        MockServerWebExchange exchange = run(MockServerHttpRequest.get("/api/v1/payments/create")
                .header("Authorization", "Bearer " + token("user_a", "https://evil.example", Instant.now().plusSeconds(60))));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair attacker = generator.generateKeyPair();
        String forged = JWT.create().withKeyId(KID).withSubject("user_a").withIssuer(ISSUER)
                .withExpiresAt(Instant.now().plusSeconds(60))
                .sign(Algorithm.RSA256((RSAPublicKey) attacker.getPublic(), (RSAPrivateKey) attacker.getPrivate()));

        MockServerWebExchange exchange = run(MockServerHttpRequest.get("/api/v1/payments/create")
                .header("Authorization", "Bearer " + forged));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void publicPathPassesWithoutTokenButLosesClientUserHeader() {
        run(MockServerHttpRequest.get("/api/v1/public/notifications/status").header("X-User-Id", "victim"));

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders().containsKey("X-User-Id")).isFalse();
    }

    @Test
    void notificationStreamRequiresAToken() {
        MockServerWebExchange exchange = run(MockServerHttpRequest.get("/api/v1/notifications/stream"));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void corsPreflightIsNotAuthenticated() {
        run(MockServerHttpRequest.options("/api/v1/payments/create"));

        assertThat(forwarded.get()).isNotNull();
    }
}
