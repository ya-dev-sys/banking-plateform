package com.yanis.api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.yanis.api_gateway.filter.LoggingFilter;
import com.yanis.api_gateway.filter.RateLimitFilter;

/**
 * Gateway configuration for routes and filters.
 *
 * <p>
 * Configures:
 * <ul>
 * <li>Routes to backend microservices</li>
 * <li>Custom filters (Authentication, Logging, RateLimit)</li>
 * <li>Circuit breaker and retry logic</li>
 * </ul>
 */
@Configuration
@org.springframework.context.annotation.Profile("!test")
public class GatewayConfig {

        private final LoggingFilter loggingFilter;
        private final RateLimitFilter rateLimitFilter;

        public GatewayConfig(LoggingFilter loggingFilter, RateLimitFilter rateLimitFilter) {
                this.loggingFilter = loggingFilter;
                this.rateLimitFilter = rateLimitFilter;
        }

        /**
         * Configures Gateway routes with custom filters.
         *
         * <p>
         * Routes are defined programmatically with filters applied in order:
         * <ol>
         * <li>LoggingFilter - Request/response logging</li>
         * <li>RateLimitFilter - Rate limiting per user</li>
         * </ol>
         *
         * @param builder RouteLocatorBuilder for building routes.
         * @return Configured RouteLocator.
         */
        @Bean
        public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
                return builder.routes()
                                // Auth Service Route
                                .route("auth-service", r -> r
                                                .path("/api/auth/**")
                                                .filters(f -> f
                                                                .stripPrefix(1)
                                                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                                                .filter(rateLimitFilter
                                                                                .apply(new RateLimitFilter.Config()))
                                                                .circuitBreaker(config -> config
                                                                                .setName("authServiceCircuitBreaker")
                                                                                .setFallbackUri("forward:/fallback/auth"))
                                                                .retry(config -> config
                                                                                .setRetries(3)
                                                                                .setStatuses(org.springframework.http.HttpStatus.BAD_GATEWAY,
                                                                                                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)))
                                                .uri("lb://AUTH-SERVICE"))

                                // Future routes for other services can be added here
                                // Example: Account Service
                                // .route("account-service", r -> r
                                // .path("/api/accounts/**")
                                // .filters(f -> f
                                // .stripPrefix(1)
                                // .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                // .filter(rateLimitFilter.apply(new RateLimitFilter.Config()))
                                // .filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                                // .uri("lb://ACCOUNT-SERVICE"))

                                .build();
        }
}
