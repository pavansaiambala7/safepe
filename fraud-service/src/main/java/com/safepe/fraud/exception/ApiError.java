package com.safepe.fraud.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * The single error shape returned by every SafePe service.
 * <p>
 * Mirrors com.safepe.gateway.error.ApiError so the frontend parses one type
 * regardless of which tier produced the failure. The traceId is the only
 * server-side detail exposed - the stack trace stays in the logs, correlated
 * by that same id.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String code,
        String message,
        int status,
        String path,
        String traceId,
        Instant timestamp,
        Map<String, String> fieldErrors
) {
    public static ApiError of(String code, String message, int status,
                              String path, String traceId) {
        return new ApiError(code, message, status, path, traceId, Instant.now(), null);
    }

    public static ApiError validation(String message, int status, String path,
                                      String traceId, Map<String, String> fieldErrors) {
        return new ApiError("VALIDATION_FAILED", message, status, path, traceId,
                Instant.now(), fieldErrors);
    }
}
