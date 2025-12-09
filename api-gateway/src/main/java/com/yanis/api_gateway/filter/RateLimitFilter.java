package com.yanis.api_gateway.filter;

import java.net.URI;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

/**
 * Rate limiting filter using Redis sliding window.
 *
 * <p>
 * Limits requests per user (or IP if anonymous) to prevent abuse.
 * Uses Redis for distributed rate limiting across multiple gateway instances.
 * </p>
 */
@Component
public class RateLimitFilter extends AbstractGatewayFilterFactory<RateLimitFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gateway.rate-limit.requests:100}")
    private int rateLimit;

    @Value("${gateway.rate-limit.window-seconds:60}")
    private int windowSizeSeconds;

    public RateLimitFilter(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
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
                redisTemplate.expire(redisKey, windowSizeSeconds, TimeUnit.SECONDS);
            }

            // Calculate remaining requests
            long remaining = Math.max(0, rateLimit - currentCount);

            // Calculate reset time
            Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            Instant resetTime = Instant.now().plusSeconds(ttl != null ? ttl : windowSizeSeconds);

            // Add rate limit headers
            exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(rateLimit));
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(remaining));
            exchange.getResponse().getHeaders().add("X-RateLimit-Reset", String.valueOf(resetTime.getEpochSecond()));

            // Check if rate limit exceeded
            if (currentCount > rateLimit) {
                logger.warn("Rate limit exceeded for user: {} (count: {})", userKey, currentCount);
                return onError(exchange, "Rate limit exceeded. Try again later.", HttpStatus.TOO_MANY_REQUESTS);
            }

            logger.debug("Rate limit check passed for user: {} ({}/{})", userKey, currentCount, rateLimit);

            return chain.filter(exchange);
        };
    }

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

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, message);
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setInstance(URI.create(exchange.getRequest().getPath().toString()));
        problemDetail.setProperty("timestamp", Instant.now());

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(problemDetail);
        } catch (JsonProcessingException e) {
            logger.error("Error writing JSON response", e);
            bytes = "{\"title\":\"Internal Server Error\",\"status\":500}".getBytes();
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    public static class Config {
    }
}
