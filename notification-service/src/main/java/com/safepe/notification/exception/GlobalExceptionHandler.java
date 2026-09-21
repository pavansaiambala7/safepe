package com.safepe.notification.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Global error middleware for notification-service.
 * <p>
 * Slimmer than the payment/fraud equivalents on purpose: this service has
 * neither JPA nor the bean-validation starter on its classpath, so the
 * DataIntegrityViolation and ConstraintViolation handlers would not compile.
 * <p>
 * Note on SSE: an exception raised after the SseEmitter has been returned
 * cannot be intercepted here, because the response is already committed by
 * then. Those failures are surfaced via SseEmitter#completeWithError inside
 * NotificationSSEService instead.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(SafePeException.class)
    public ResponseEntity<ApiError> handleSafePe(SafePeException ex, HttpServletRequest req) {
        String traceId = newTraceId();
        if (ex.getStatus().is5xxServerError()) {
            log.error("[{}] {} {} -> {} {}", traceId, req.getMethod(), req.getRequestURI(),
                    ex.getStatus().value(), ex.getMessage(), ex);
        } else {
            log.warn("[{}] {} {} -> {} {}", traceId, req.getMethod(), req.getRequestURI(),
                    ex.getStatus().value(), ex.getMessage());
        }
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), req, traceId);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex,
                                                     HttpServletRequest req) {
        String traceId = newTraceId();
        log.warn("[{}] unreadable request body on {}", traceId, req.getRequestURI());
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "Request body is missing or is not valid JSON.", req, traceId);
    }

    @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class})
    public ResponseEntity<ApiError> handleBadArgument(RuntimeException ex,
                                                      HttpServletRequest req) {
        String traceId = newTraceId();
        log.warn("[{}] rejected request on {} {}: {}", traceId, req.getMethod(),
                req.getRequestURI(), ex.toString());
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "Request is missing a required field or contains an invalid value.",
                req, traceId);
    }

    @ExceptionHandler({NoSuchElementException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(Exception ex, HttpServletRequest req) {
        String traceId = newTraceId();
        log.warn("[{}] not found: {} {}", traceId, req.getMethod(), req.getRequestURI());
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND",
                "The requested resource does not exist.", req, traceId);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                           HttpServletRequest req) {
        String traceId = newTraceId();
        log.warn("[{}] method {} not allowed on {}", traceId, req.getMethod(),
                req.getRequestURI());
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "HTTP " + req.getMethod() + " is not supported on this endpoint.", req, traceId);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest req) {
        String traceId = newTraceId();
        log.error("[{}] UNHANDLED on {} {}", traceId, req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred. Quote the traceId when reporting this.",
                req, traceId);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
                                           HttpServletRequest req, String traceId) {
        return ResponseEntity.status(status).body(
                ApiError.of(code, message, status.value(), req.getRequestURI(), traceId));
    }

    private String newTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
