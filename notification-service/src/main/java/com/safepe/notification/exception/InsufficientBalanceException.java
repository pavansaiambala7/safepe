package com.safepe.notification.exception;

import org.springframework.http.HttpStatus;

/**
 * Request is well-formed and authorised, but the account cannot fund it.
 */
public class InsufficientBalanceException extends SafePeException {

    public InsufficientBalanceException(String message) {
        super("INSUFFICIENT_BALANCE", message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public InsufficientBalanceException(String message, Throwable cause) {
        super("INSUFFICIENT_BALANCE", message, HttpStatus.UNPROCESSABLE_ENTITY, cause);
    }
}
