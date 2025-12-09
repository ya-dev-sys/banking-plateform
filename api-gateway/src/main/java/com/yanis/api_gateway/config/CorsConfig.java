package com.yanis.api_gateway.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * CORS configuration for API Gateway.
 *
 * <p>
 * Configures Cross-Origin Resource Sharing to allow frontend applications
 * to access the API from different origins.
 * </p>
 *
 * <p>
 * Allowed origins:
 * <ul>
 * <li>http://localhost:3000 (React development)</li>
 * <li>http://localhost:4200 (Angular development)</li>
 * </ul>
 */
@Configuration
public class CorsConfig {

    /**
     * Configures CORS filter for Gateway.
     *
     * <p>
     * Allows specific origins, methods, and headers for cross-origin requests.
     * </p>
     *
     * @return Configured CorsWebFilter.
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Allowed origins (frontend applications)
        corsConfig.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000", // React
                "http://localhost:4200" // Angular
        ));

        // Allowed HTTP methods
        corsConfig.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Allowed headers
        corsConfig.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin"));

        // Exposed headers (visible to frontend)
        corsConfig.setExposedHeaders(Arrays.asList(
                "X-RateLimit-Limit",
                "X-RateLimit-Remaining",
                "X-RateLimit-Reset"));

        // Allow credentials (cookies, authorization headers)
        corsConfig.setAllowCredentials(true);

        // Max age for preflight requests (1 hour)
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
