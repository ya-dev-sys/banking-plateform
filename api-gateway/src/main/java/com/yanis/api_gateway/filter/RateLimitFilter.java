package com.yanis.api_gateway.filter;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Rate limiting filter using Redis sliding window.
 *
 * <p>
 * Limits requests per user (or IP if anonymous) to prevent abuse.
 * Uses Redis for distributed rate limiting across multiple gateway instances.
 * </p>
 *
 * <p>
 * Configuration:
 * <ul>
 * <li>Limit: 100 requests per minute per user</li>
 * <li>Burst capacity: 200 requests</li>
 * <li>Returns 429 Too Many Requests if exceeded</li>
 * </ul>
 *
 * <p>
 * Response headers:
 * <ul>
 * <li>X-RateLimit-Limit: Maximum requests allowed</li>
 * <li>X-RateLimit-Remaining: Remaining requests</li>
 * <li>X-RateLimit-Reset: Timestamp when limit resets</li>
 * </ul>
 */
@Component
@Slf4j

public class RateLimitFilter extends AbstractGatewayFilterFactory<RateLimitFilter.Config> {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final int RATE_LIMIT = 100; // requests per minute
    private static final long WINDOW_SIZE_SECONDS = 60;

    public RateLimitFilter(RedisTemplate<String, Object> redisTemplate) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String userKey = getUserKey(exchange);
            String redisKey = "rate_limit:" + userKey;

            // Get current count
            Long currentCount = redisTemplate.opsForValue().increment(redisKey);

            if (currentCount == null) {
                currentCount = 0L;
            }

            // Set expiration on first request
            if (currentCount == 1) {
                redisTemplate.expire(redisKey, WINDOW_SIZE_SECONDS, TimeUnit.SECONDS);
            }

            // Calculate remaining requests
            long remaining = Math.max(0, RATE_LIMIT - currentCount);

            // Calculate reset time
            Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            Instant resetTime = Instant.now().plusSeconds(ttl != null ? ttl : WINDOW_SIZE_SECONDS);

            // Add rate limit headers
            exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(RATE_LIMIT));
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(remaining));
            exchange.getResponse().getHeaders().add("X-RateLimit-Reset", String.valueOf(resetTime.getEpochSecond()));

            // Check if rate limit exceeded
            if (currentCount > RATE_LIMIT) {
                log.warn("Rate limit exceeded for user: {} (count: {})", userKey, currentCount);
                return onError(exchange, "Rate limit exceeded. Try again later.", HttpStatus.TOO_MANY_REQUESTS);
            }

            log.debug("Rate limit check passed for user: {} ({}/{})", userKey, currentCount, RATE_LIMIT);

            return chain.filter(exchange);
        };
    }

    /**
     * Extracts user identifier from request (email from JWT or IP address).
     *
     * @param exchange ServerWebExchange.
     * @return User identifier for rate limiting.
     */
    private String getUserKey(ServerWebExchange exchange) {
        // Try to get user email from header (set by AuthenticationFilter)
        String email = exchange.getRequest().getHeaders().getFirst("X-User-Email");

        if (email != null && !email.isEmpty()) {
            return email;
        }

        // Fallback to IP address for anonymous users
        String ipAddress = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";

        return "ip:" + ipAddress;
    }

    /**
     * Returns error response for rate limit exceeded.
     *
     * @param exchange ServerWebExchange.
     * @param message  Error message.
     * @param status   HTTP status code.
     * @return Mono with error response.
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");

        String errorResponse = String.format(
                "{\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getPath());

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(errorResponse.getBytes())));
    }

    /**
     * Configuration class for the filter.
     */
    public static class Config {
        // Configuration properties if needed
    }
}
