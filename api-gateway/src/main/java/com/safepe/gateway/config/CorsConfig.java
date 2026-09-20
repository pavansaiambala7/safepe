package com.safepe.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS as an explicit {@link CorsWebFilter} rather than the gateway's
 * {@code spring.cloud.gateway.globalcors} block.
 * <p>
 * The reason matters: {@code globalcors} configures the route handler mapping,
 * which is bypassed when a filter short-circuits the chain. JwtAuthFilter (401)
 * and RateLimitFilter (429) both do exactly that, so their responses carried no
 * {@code Access-Control-Allow-Origin} header and the browser reported them as
 * generic CORS failures instead of the real status. A WebFilter at
 * HIGHEST_PRECEDENCE wraps the entire chain, so every response is covered.
 * <p>
 * The {@code globalcors} block has been removed from application.yml — keeping
 * both would emit duplicate Access-Control-Allow-Origin headers, which browsers
 * reject outright.
 */
@Configuration
@Slf4j
public class CorsConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter corsWebFilter(
            @Value("${safepe.cors.allowed-origins}") String allowedOrigins) {

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        CorsConfiguration config = new CorsConfiguration();
        // Explicit origins, never "*": allowCredentials(true) makes the wildcard
        // illegal and the browser rejects the response.
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of(
                "X-RateLimit-Limit", "X-RateLimit-Remaining", "Retry-After"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        log.info("CORS enabled for origins: {}", origins);
        return new CorsWebFilter(source);
    }
}
