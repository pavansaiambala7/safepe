package com.safepe.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Razorpay credentials, bound once from {@code safepe.razorpay.*} in application.yml
 * (which reads RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET and RAZORPAY_WEBHOOK_SECRET).
 * <p>
 * Being a record, it is bound through its constructor and is immutable. Classes
 * that need it receive it through their own constructor, replacing the
 * {@code @Value} fields that were previously repeated in three classes.
 *
 * @param keyId         public key id, also sent to the browser for checkout
 * @param keySecret     API secret, used for server-side calls
 * @param webhookSecret secret Razorpay signs webhook payloads with
 */
@ConfigurationProperties(prefix = "safepe.razorpay")
public record RazorpayProperties(String keyId, String keySecret, String webhookSecret) {
}
