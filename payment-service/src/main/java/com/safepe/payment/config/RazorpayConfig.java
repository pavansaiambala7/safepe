package com.safepe.payment.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RazorpayProperties.class)
@Slf4j
public class RazorpayConfig {

    @Bean
    public RazorpayClient razorpayClient(RazorpayProperties razorpay) {
        try {
            log.info("💳 Initializing Razorpay Client with Key ID: {}", razorpay.keyId());
            return new RazorpayClient(razorpay.keyId(), razorpay.keySecret());
        } catch (RazorpayException e) {
            log.error("❌ Failed to initialize Razorpay Client: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize payment gateway", e);
        }
    }
}
