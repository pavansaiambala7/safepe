package com.safepe.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Layer 2 — Razorpay Configuration
 * ================================
 * This creates the main tool for talking to Razorpay's servers.
 * By marking it @Bean, Spring Boot will make it available to any Service that needs it.
 */
@Configuration
@Slf4j
public class RazorpayConfig {

    @Value("${safepe.razorpay.key-id}")
    private String keyId;

    @Value("${safepe.razorpay.key-secret}")
    private String keySecret;

    @Bean
    public RazorpayClient razorpayClient() {
        try {
            log.info("💳 Initializing Razorpay Client with Key ID: {}", keyId);
            return new RazorpayClient(keyId, keySecret);
        } catch (RazorpayException e) {
            log.error("❌ Failed to initialize Razorpay Client. Please check API keys.", e);
            throw new RuntimeException("Failed to initialize payment gateway", e);
        }
    }
}
