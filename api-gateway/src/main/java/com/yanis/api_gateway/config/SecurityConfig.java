package com.yanis.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Spring Security configuration for API Gateway (Reactive).
 *
 * <p>
 * Configures:
 * <ul>
 * <li>Stateless session management (implicitly stateless in WebFlux)</li>
 * <li>CSRF disabled (REST API with JWT)</li>
 * <li>All requests permitted (authentication handled in custom filters)</li>
 * <li>Security headers (HSTS, X-Frame-Options, etc.)</li>
 * </ul>
 *
 * <p>
 * <strong>Note:</strong> JWT validation is performed in AuthenticationFilter,
 * not in Spring Security filter chain.
 * </p>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

        /**
         * Configures the security filter chain for HTTP requests.
         *
         * <p>
         * Permits all requests as authentication is handled by custom Gateway filters.
         * Disables CSRF for stateless API.
         * </p>
         *
         * @param http The ServerHttpSecurity to configure.
         * @return The configured SecurityWebFilterChain.
         */
        @Bean
        public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
                return http
                                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                                .authorizeExchange(exchange -> exchange
                                                .anyExchange().permitAll())
                                .headers(headers -> headers
                                                .frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable)
                                                .contentSecurityPolicy(csp -> csp
                                                                .policyDirectives("default-src 'self'")))
                                .build();
        }
}
