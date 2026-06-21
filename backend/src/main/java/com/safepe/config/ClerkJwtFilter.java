package com.safepe.config;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

/**
 * Clerk JWT verification filter.
 * Sits at the security boundary — Layer 1.
 * Validates every incoming request's Bearer token against Clerk's JWKS.
 */
@Component
@Slf4j
public class ClerkJwtFilter extends OncePerRequestFilter {

    private final JwkProvider jwkProvider;
    private final String issuer;

    public ClerkJwtFilter(
            @Value("${safepe.clerk.jwks-url}") String jwksUrl,
            @Value("${safepe.clerk.issuer}") String issuer) {
        this.jwkProvider = new UrlJwkProvider(toURL(jwksUrl));
        this.issuer = issuer;
    }

    private static URL toURL(String url) {
        try { return new URL(url); }
        catch (Exception e) { throw new RuntimeException("Invalid JWKS URL: " + url, e); }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/api/health")
            || path.startsWith("/api/webhooks")
            || path.startsWith("/api/v1/public");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            DecodedJWT decoded = JWT.decode(token);
            Jwk jwk = jwkProvider.get(decoded.getKeyId());

            Algorithm algorithm = Algorithm.RSA256(
                (RSAPublicKey) jwk.getPublicKey(), null);

            JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .build();

            DecodedJWT verified = verifier.verify(token);
            String userId = verified.getSubject();

            var auth = new UsernamePasswordAuthenticationToken(
                userId, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Authenticated user: {}", userId);

        } catch (Exception e) {
            log.warn("JWT verification failed: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Invalid or expired token\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
