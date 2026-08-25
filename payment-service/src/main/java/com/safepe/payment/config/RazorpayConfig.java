package com.safepe.payment.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RazorpayConfig {

    @Value("${safepe.razorpay.key-id:rzp_test_T5AtiMDfqh5J2N}")
    private String keyId;

    @Value("${safepe.razorpay.key-secret:dummy_secret}")
    private String keySecret;

    @Bean
    public RazorpayClient razorpayClient() {
        try {
            log.info("💳 Initializing Razorpay Client with Key ID: {}", keyId);
            return new RazorpayClient(keyId, keySecret);
        } catch (RazorpayException e) {
            log.error("❌ Failed to initialize Razorpay Client: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize payment gateway", e);
        }
    }
}
