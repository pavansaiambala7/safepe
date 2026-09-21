package com.safepe.notification.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for errors this service raises deliberately.
 * <p>
 * Carrying the status and a stable machine-readable code on the exception lets
 * GlobalExceptionHandler translate it without a growing if/else chain, and lets
 * the frontend branch on the code instead of parsing English prose.
 */
@Getter
public class SafePeException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public SafePeException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public SafePeException(String code, String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }
}
