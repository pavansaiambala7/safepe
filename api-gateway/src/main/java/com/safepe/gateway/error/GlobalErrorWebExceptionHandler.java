package com.safepe.gateway.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * Global error middleware for the API Gateway.
 * <p>
 * The gateway is Spring WebFlux, so {@code @RestControllerAdvice} does not
 * apply here — errors must be intercepted at the {@code ErrorWebExceptionHandler}
 * level. {@code @Order(-2)} places this ahead of Boot's
 * {@code DefaultErrorWebExceptionHandler} (which sits at -1).
 * <p>
 * Without this, a downstream service that is not registered in Eureka produces
 * a bare 503 with an HTML body that the SPA cannot parse.
 */
@Component
@Order(-2)
@Slf4j
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalErrorWebExceptionHandler(ErrorAttributes errorAttributes,
                                          WebProperties webProperties,
                                          ApplicationContext applicationContext,
                                          ServerCodecConfigurer codecConfigurer,
                                          ObjectMapper objectMapper) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        this.objectMapper = objectMapper;
        super.setMessageWriters(codecConfigurer.getWriters());
        super.setMessageReaders(codecConfigurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::render);
    }

    private Mono<ServerResponse> render(ServerRequest request) {
        Throwable error = getError(request);
        String path = request.path();
        String traceId = UUID.randomUUID().toString().substring(0, 8);

        Resolved resolved = resolve(error);

        if (resolved.status().is5xxServerError()) {
            log.error("[{}] {} {} -> {} : {}", traceId, request.method(), path,
                    resolved.status().value(), error.toString(), error);
        } else {
            log.warn("[{}] {} {} -> {} : {}", traceId, request.method(), path,
                    resolved.status().value(), error.getMessage());
        }

        ApiError body = ApiError.of(resolved.code(), resolved.message(),
                resolved.status().value(), path, traceId);

        return ServerResponse.status(resolved.status())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }

    private Resolved resolve(Throwable error) {
        if (error instanceof NotFoundException) {
            // Route matched, but no instance of that service is registered in Eureka.
            return new Resolved(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                    "The requested service is temporarily unavailable. Please retry shortly.");
        }
        if (error instanceof ResponseStatusException rse) {
            HttpStatus status = HttpStatus.resolve(rse.getStatusCode().value());
            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            return new Resolved(status, status.name(),
                    rse.getReason() != null ? rse.getReason() : status.getReasonPhrase());
        }
        if (error instanceof TimeoutException) {
            return new Resolved(HttpStatus.GATEWAY_TIMEOUT, "UPSTREAM_TIMEOUT",
                    "The upstream service did not respond in time.");
        }
        if (error instanceof ConnectException || error instanceof IOException) {
            return new Resolved(HttpStatus.BAD_GATEWAY, "UPSTREAM_UNREACHABLE",
                    "Could not reach the upstream service.");
        }
        // Never echo the raw exception message to the caller — it leaks internals.
        return new Resolved(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred. Quote the traceId when reporting this.");
    }

    private record Resolved(HttpStatus status, String code, String message) {}

    /** Shared helper so filters can write the same body without duplicating Jackson wiring. */
    public static Mono<Void> writeError(org.springframework.web.server.ServerWebExchange exchange,
                                        ObjectMapper mapper,
                                        HttpStatus status,
                                        String code,
                                        String message) {
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String traceId = UUID.randomUUID().toString().substring(0, 8);
        ApiError body = ApiError.of(code, message, status.value(),
                exchange.getRequest().getPath().value(), traceId);

        byte[] bytes;
        try {
            bytes = mapper.writeValueAsBytes(body);
        } catch (Exception e) {
            // Escaping matters: a quote in `message` would otherwise emit invalid JSON.
            bytes = ("{\"code\":\"" + code + "\",\"status\":" + status.value()
                    + ",\"traceId\":\"" + traceId + "\"}").getBytes(StandardCharsets.UTF_8);
        }
        DataBufferFactory factory = response.bufferFactory();
        return response.writeWith(Mono.just(factory.wrap(bytes)));
    }
}
