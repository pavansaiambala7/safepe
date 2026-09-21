package com.safepe.payment.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Global error middleware for payment-service.
 * <p>
 * Before this existed, an unmapped exception fell through to the default
 * /error endpoint, which answered 500 with a body exposing the exception
 * message and package structure. Every failure now returns the same ApiError
 * envelope, and internal detail is replaced by a traceId that correlates to
 * the full stack trace in the logs.
 * <p>
 * Ordering note: more specific handlers win over broader ones regardless of
 * declaration order, so the catch-all at the bottom only fires for genuinely
 * unanticipated failures.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── Deliberate, domain-level failures ───────────────────────────────────

    @ExceptionHandler(SafePeException.class)
    public ResponseEntity<ApiError> handleSafePe(SafePeException ex, HttpServletRequest req) {
        String traceId = newTraceId();
        // 5xx means we are at fault, so keep the stack. 4xx belongs to the
        // caller and would only add log noise.
        if (ex.getStatus().is5xxServerError()) {
            log.error("[{}] {} {} -> {} {}", traceId, req.getMethod(), req.getRequestURI(),
                    ex.getStatus().value(), ex.getMessage(), ex);
        } else {
            log.warn("[{}] {} {} -> {} {}", traceId, req.getMethod(), req.getRequestURI(),
                    ex.getStatus().value(), ex.getMessage());
        }
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), req, traceId);
    }

    // ── Bad input from the caller ───────────────────────────────────────────

    /** Fires when a @Valid @RequestBody DTO fails bean validation. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest req) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                fields.putIfAbsent(fe.getField(),
                        fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "is invalid"));
        ex.getBindingResult().getGlobalErrors().forEach(ge ->
                fields.putIfAbsent(ge.getObjectName(),
                        ge.getDefaultMessage() != null ? ge.getDefaultMessage() : "is invalid"));

        String traceId = newTraceId();
        log.warn("[{}] validation failed on {} {}: {}", traceId, req.getMethod(),
                req.getRequestURI(), fields);

        return ResponseEntity.badRequest().body(ApiError.validation(
                "One or more fields are invalid.", HttpStatus.BAD_REQUEST.value(),
                req.getRequestURI(), traceId, fields));
    }

    /** Validation on @RequestParam / @PathVariable, which bypasses the binding result. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex,
                                                     HttpServletRequest req) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v ->
                fields.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));

        String traceId = newTraceId();
        log.warn("[{}] constraint violation on {}: {}", traceId, req.getRequestURI(), fields);

        return ResponseEntity.badRequest().body(ApiError.validation(
                "One or more parameters are invalid.", HttpStatus.BAD_REQUEST.value(),
                req.getRequestURI(), traceId, fields));
    }

    /** Malformed JSON, or a body that cannot be coerced into the target type. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex,
                                                     HttpServletRequest req) {
        String traceId = newTraceId();
        log.warn("[{}] unreadable request body on {}: {}", traceId, req.getRequestURI(),
                ex.getMostSpecificCause().getMessage());
        // Echoing the parser message would leak internal type names.
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "Request body is missing or is not valid JSON.", req, traceId);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest req) {
        String traceId = newTraceId();
        log.warn("[{}] type mismatch for {} on {}", traceId, ex.getName(), req.getRequestURI());
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "Parameter " + ex.getName() + " has the wrong type.", req, traceId);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex,
                                                       HttpServletRequest req) {
        String traceId = newTraceId();
        log.warn("[{}] missing parameter {} on {}", traceId, ex.getParameterName(),
                req.getRequestURI());
        return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
                "Required parameter " + ex.getParameterName() + " is missing.", req, traceId);
    }

    /**
     * Controllers here still read untyped Map bodies, e.g.
     * requestData.get("amount").toString(). A missing key throws NPE and a
     * non-numeric value throws NumberFormatException - both land here as 400
     * rather than a misleading 500, until those bodies become validated DTOs.
     */
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

    /**
     * Controllers that still parse dates out of raw Map bodies (BillController
     * does this for dueDate) throw DateTimeParseException, which is NOT an
     * IllegalArgumentException and so fell through to the catch-all as a 500.
     * A caller sending a bad date is a 400.
     */
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ApiError> handleBadDate(DateTimeParseException ex,
                                                  HttpServletRequest req) {
        String traceId = newTraceId();
        log.warn("[{}] unparseable date on {}: {}", traceId, req.getRequestURI(),
                ex.getParsedString());
        return build(HttpStatus.BAD_REQUEST, "INVALID_DATE",
                "A date value is not in the expected format (yyyy-MM-dd).", req, traceId);
    }

    // ── Not found / not allowed ─────────────────────────────────────────────

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

    // ── Persistence ─────────────────────────────────────────────────────────

    /** Unique/foreign-key violations. The raw SQL message would expose the schema. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
                                                        HttpServletRequest req) {
        String traceId = newTraceId();
        log.error("[{}] data integrity violation on {}: {}", traceId, req.getRequestURI(),
                ex.getMostSpecificCause().getMessage(), ex);
        return build(HttpStatus.CONFLICT, "CONFLICT",
                "The request conflicts with existing data.", req, traceId);
    }

    // ── Catch-all ───────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest req) {
        String traceId = newTraceId();
        log.error("[{}] UNHANDLED on {} {}", traceId, req.getMethod(), req.getRequestURI(), ex);
        // Deliberately generic: ex.getMessage() has leaked driver, provider and
        // package detail in the past. The traceId is the bridge to the logs.
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred. Quote the traceId when reporting this.",
                req, traceId);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
                                           HttpServletRequest req, String traceId) {
        return ResponseEntity.status(status).body(
                ApiError.of(code, message, status.value(), req.getRequestURI(), traceId));
    }

    private String newTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
