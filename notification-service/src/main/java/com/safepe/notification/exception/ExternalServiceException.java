package com.safepe.notification.exception;

import org.springframework.http.HttpStatus;

/**
 * A third party (Razorpay, Gemini) failed or timed out. Wraps their exception
 * so provider internals never reach the client.
 */
public class ExternalServiceException extends SafePeException {

    public ExternalServiceException(String message) {
        super("EXTERNAL_SERVICE_ERROR", message, HttpStatus.BAD_GATEWAY);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super("EXTERNAL_SERVICE_ERROR", message, HttpStatus.BAD_GATEWAY, cause);
    }
}
