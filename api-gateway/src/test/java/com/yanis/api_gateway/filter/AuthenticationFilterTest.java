package com.yanis.api_gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanis.api_gateway.security.JwtTokenProvider;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Tests unitaires pour le filtre d'authentification
 * {@link AuthenticationFilter}.
 *
 * <p>
 * Ce filtre est critique pour la sécurité car il valide les tokens JWT
 * entrants.
 * Les tests vérifient que le filtre rejette correctement les requêtes non
 * authentifiées
 * et propage le contexte utilisateur pour les requêtes valides.
 * </p>
 *
 * @see AuthenticationFilter
 * @see JwtTokenProvider
 */
class AuthenticationFilterTest {

    private AuthenticationFilter authenticationFilter;
    private JwtTokenProvider jwtTokenProvider;
    private ObjectMapper objectMapper;
    private GatewayFilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        authenticationFilter = new AuthenticationFilter(jwtTokenProvider, objectMapper);
        filterChain = mock(GatewayFilterChain.class);
        when(filterChain.filter(org.mockito.ArgumentMatchers.any(ServerWebExchange.class)))
                .thenReturn(Mono.empty());
    }

    /**
     * Vérifie que les endpoints publics sont accessibles sans token.
     *
     * <p>
     * Le filtre doit laisser passer les requêtes vers /api/auth/* sans validation
     * JWT
     * pour permettre le login et l'inscription.
     * </p>
     */
    @Test
    void shouldSkipAuthenticationForPublicEndpoints() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = authenticationFilter.apply(new AuthenticationFilter.Config());

        Mono<Void> result = filter.filter(exchange, filterChain);

        StepVerifier.create(result)
                .verifyComplete();
    }

    /**
     * Vérifie le rejet des requêtes sans header Authorization.
     *
     * <p>
     * Si le header est manquant, le filtre doit retourner 401 Unauthorized
     * immediatement
     * pour protéger les ressources sécurisées.
     * </p>
     */
    @Test
    void shouldReturnUnauthorizedWhenHeaderIsMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/secure").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = authenticationFilter.apply(new AuthenticationFilter.Config());

        Mono<Void> result = filter.filter(exchange, filterChain);

        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    /**
     * Vérifie le rejet des tokens invalides ou expirés.
     *
     * <p>
     * Utilise {@link JwtTokenProvider#validateToken(String)} pour la validation.
     * Doit retourner 401 si le provider retourne false.
     * </p>
     */
    @Test
    void shouldReturnUnauthorizedWhenTokenIsInvalid() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/secure")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = authenticationFilter.apply(new AuthenticationFilter.Config());

        when(jwtTokenProvider.validateToken(anyString())).thenReturn(false);

        Mono<Void> result = filter.filter(exchange, filterChain);

        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    /**
     * Vérifie l'injection des headers utilisateur après authentification réussie.
     *
     * <p>
     * Le filtre doit extraire l'email et les rôles du token et les ajouter
     * aux headers `X-User-Email` et `X-User-Roles` pour les microservices avals.
     * </p>
     */
    @Test
    void shouldInjectUserHeadersWhenTokenIsValid() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/secure")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = authenticationFilter.apply(new AuthenticationFilter.Config());

        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.extractEmail("valid-token")).thenReturn("user@example.com");
        when(jwtTokenProvider.extractRoles("valid-token")).thenReturn(List.of("ROLE_USER"));

        // Capture the mutated exchange
        GatewayFilterChain capturingChain = mutatedExchange -> {
            String email = mutatedExchange.getRequest().getHeaders().getFirst("X-User-Email");
            String roles = mutatedExchange.getRequest().getHeaders().getFirst("X-User-Roles");

            assertEquals("user@example.com", email);
            assertEquals("ROLE_USER", roles);
            return Mono.empty();
        };

        filter.filter(exchange, capturingChain).block();
    }
}
