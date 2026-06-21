package com.safepe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SafePe — AI-powered UPI fraud detection and secure payments platform.
 *
 * <p>Monolithic Spring Boot application with a 3-layer security architecture:
 * <ol>
 *   <li>Clerk JWT authentication</li>
 *   <li>Razorpay payment verification</li>
 *   <li>Gemini AI fraud-risk scoring</li>
 * </ol>
 */
@SpringBootApplication
public class SafePeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SafePeApplication.class, args);
    }
}
