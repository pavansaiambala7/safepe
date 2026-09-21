package com.safepe.gateway.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * The single error shape returned by every SafePe component.
 * <p>
 * The frontend can parse one type for 401, 429, 502, 503 and 500 alike.
 * {@code traceId} is the only server-side detail exposed to the caller — the
 * stack trace stays in the logs, correlated by the same id.
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
    public static ApiError of(String code, String message, int status, String path, String traceId) {
        return new ApiError(code, message, status, path, traceId, Instant.now(), null);
    }
}
