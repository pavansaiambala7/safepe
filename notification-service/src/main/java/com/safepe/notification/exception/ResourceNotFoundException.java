package com.safepe.notification.exception;

import org.springframework.http.HttpStatus;

/**
 * Requested entity does not exist.
 */
public class ResourceNotFoundException extends SafePeException {

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super("RESOURCE_NOT_FOUND", message, HttpStatus.NOT_FOUND, cause);
    }
}
