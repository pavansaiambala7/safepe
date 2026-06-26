package com.safepe.layer1.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Security Layer — API Rate Limiter
 * ==================================
 * This filter sits directly in front of our Controllers.
 * It tracks IP addresses and blocks hackers who try to spam our APIs.
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    // A memory map storing the Token Buckets for each IP address
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        // 1. SKIP Webhooks! We never rate limit Razorpay's automatic pings.
        if (requestUri.contains("/webhook")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. ONLY Rate Limit the critical endpoints (Fraud checks & Payment Creation)
        if (requestUri.startsWith("/api/v1/fraud") || requestUri.startsWith("/api/v1/payments/create")) {
            
            // Get the user's IP Address
            String ipAddress = request.getRemoteAddr();
            
            // Fetch their personal bucket, or create a new one if this is their first visit
            Bucket bucket = buckets.computeIfAbsent(ipAddress, this::createNewBucket);

            // Try to consume 1 token from the bucket
            if (bucket.tryConsume(1)) {
                // Success! They have tokens left. Let them into the Controller.
                filterChain.doFilter(request, response);
            } else {
                // Bucket is empty! Hacker detected. Block them instantly.
                log.warn("🚨 RATE LIMIT EXCEEDED: Blocking IP Address: {}", ipAddress);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too many requests. Please wait 60 seconds and try again.\"}");
                return; // Stop the request here. Do not call the Controller.
            }
        } else {
            // For all other routes (like /api/v1/vault), just let them through normally
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Creates a strict bucket rule:
     * - Maximum 5 tokens in the bucket.
     * - Refills exactly 5 tokens every 1 minute.
     */
    private Bucket createNewBucket(String key) {
        Refill refill = Refill.intervally(500, Duration.ofMinutes(1));
        Bandwidth limit = Bandwidth.classic(500, refill);
        return Bucket.builder().addLimit(limit).build();
    }
}
