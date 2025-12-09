package com.yanis.api_gateway.filter;

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.yanis.api_gateway.security.JwtTokenProvider;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Authentication filter for JWT validation.
 *
 * <p>
 * Validates JWT tokens from Authorization header and injects user context
 * into request headers for downstream services.
 * </p>
 *
 * <p>
 * Flow:
 * <ol>
 * <li>Extract "Authorization: Bearer {token}" header</li>
 * <li>Validate token signature and expiration</li>
 * <li>Extract user info (email, roles)</li>
 * <li>Inject headers: X-User-Email, X-User-Roles</li>
 * <li>If invalid: return 401 Unauthorized</li>
 * </ol>
 *
 * <p>
 * <strong>Note:</strong> Public endpoints (like /auth/**) bypass this filter.
 * </p>
 */
@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtTokenProvider jwtTokenProvider;

    public AuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        super(Config.class);
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().toString();

            // Skip authentication for public endpoints
            if (isPublicEndpoint(path)) {
                log.debug("Public endpoint accessed: {}", path);
                return chain.filter(exchange);
            }

            // Extract Authorization header
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or invalid Authorization header for path: {}", path);
                return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            // Validate token
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("Invalid JWT token for path: {}", path);
                return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }

            // Extract user information
            String email = jwtTokenProvider.extractEmail(token);
            List<String> roles = jwtTokenProvider.extractRoles(token);

            // Inject user context into request headers
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Email", email)
                    .header("X-User-Roles", String.join(",", roles))
                    .build();

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(mutatedRequest)
                    .build();

            log.debug("Authenticated user: {} with roles: {}", email, roles);

            return chain.filter(mutatedExchange);
        };
    }

    /**
     * Checks if the endpoint is public (no authentication required).
     *
     * @param path Request path.
     * @return true if public endpoint, false otherwise.
     */
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/") ||
                path.startsWith("/actuator/") ||
                path.equals("/health");
    }

    /**
     * Returns error response for authentication failures.
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
