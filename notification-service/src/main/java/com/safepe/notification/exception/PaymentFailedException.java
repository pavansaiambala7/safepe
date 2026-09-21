package com.safepe.notification.exception;

import org.springframework.http.HttpStatus;

/**
 * The payment provider rejected the transaction.
 */
public class PaymentFailedException extends SafePeException {

    public PaymentFailedException(String message) {
        super("PAYMENT_FAILED", message, HttpStatus.PAYMENT_REQUIRED);
    }

    public PaymentFailedException(String message, Throwable cause) {
        super("PAYMENT_FAILED", message, HttpStatus.PAYMENT_REQUIRED, cause);
    }
}
