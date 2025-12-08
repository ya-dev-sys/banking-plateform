package com.yanis.auth_service.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security configuration for Auth Service.
 *
 * <p>
 * Configures:
 * <ul>
 * <li>Stateless session management (no server-side sessions)</li>
 * <li>CSRF disabled (REST API with JWT)</li>
 * <li>Public access to /auth/**, /actuator/**, and Swagger UI endpoints</li>
 * <li>BCrypt password encoder (strength 10)</li>
 * </ul>
 *
 * @see org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Configures the security filter chain for HTTP requests.
     *
     * <p>
     * Sets up stateless session management and defines which endpoints
     * are publicly accessible vs requiring authentication.
     * </p>
     *
     * @param http The HttpSecurity to configure.
     * @return The configured SecurityFilterChain.
     * @throws Exception if configuration fails.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/**",
                                "/actuator/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**")
                        .permitAll()
                        .anyRequest().authenticated());

        return http.build();
    }

    /**
     * Provides BCrypt password encoder for hashing passwords.
     *
     * <p>
     * Uses BCrypt with default strength (10 rounds) for secure password storage.
     * </p>
     *
     * @return BCryptPasswordEncoder instance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
