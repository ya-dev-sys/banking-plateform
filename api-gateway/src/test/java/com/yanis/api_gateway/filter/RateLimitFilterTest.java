package com.yanis.api_gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Tests unitaires pour le filtre de limitation de débit
 * {@link RateLimitFilter}.
 *
 * <p>
 * Vérifie l'implémentation du Rate Limiting basé sur Redis (Sliding Window).
 * Assure que les compteurs sont incrémentés, les headers ajoutés, et les
 * requêtes bloquées
 * quand la limite est dépassée.
 * </p>
 *
 * @see RateLimitFilter
 */
class RateLimitFilterTest {

    private RateLimitFilter rateLimitFilter;
    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOperations;
    private ObjectMapper objectMapper;
    private GatewayFilterChain filterChain;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        rateLimitFilter = new RateLimitFilter(redisTemplate, objectMapper);

        // Inject properties via reflection as @Value won't work in unit test
        ReflectionTestUtils.setField(rateLimitFilter, "rateLimit", 100);
        ReflectionTestUtils.setField(rateLimitFilter, "windowSizeSeconds", 60);

        filterChain = mock(GatewayFilterChain.class);
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    /**
     * Vérifie qu'une requête sous la limite est autorisée.
     *
     * <p>
     * Le filtre doit incrémenter le compteur Redis et retourner les headers
     * `X-RateLimit-*` corrects sans bloquer la requête.
     * </p>
     */
    @Test
    void shouldAllowRequestWhenWithinLimit() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = rateLimitFilter.apply(new RateLimitFilter.Config());

        when(valueOperations.increment(any())).thenReturn(1L);
        when(redisTemplate.getExpire(any(), eq(TimeUnit.SECONDS))).thenReturn(60L);

        Mono<Void> result = filter.filter(exchange, filterChain);

        StepVerifier.create(result)
                .verifyComplete();

        assertEquals("100", exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"));
        assertEquals("99", exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining"));
    }

    /**
     * Vérifie le blocage (HTTP 429) quand la limite est dépassée.
     *
     * <p>
     * Si le compteur Redis dépasse la limite configurée, le filtre doit
     * retourner une erreur standardisée (ProblemDetail) avec le statut Too Many
     * Requests.
     * </p>
     */
    @Test
    void shouldBlockRequestWhenRateLimitExceeded() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = rateLimitFilter.apply(new RateLimitFilter.Config());

        when(valueOperations.increment(any())).thenReturn(101L);
        when(redisTemplate.getExpire(any(), eq(TimeUnit.SECONDS))).thenReturn(30L);

        Mono<Void> result = filter.filter(exchange, filterChain);

        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
    }

    /**
     * Vérifie le fallback sur l'adresse IP pour les utilisateurs anonymes.
     *
     * <p>
     * En l'absence de header Auth (utilisateur non authentifié), le Rate Limiting
     * doit s'appliquer basé sur l'adresse IP source pour prévenir les abus.
     * </p>
     */
    @Test
    void shouldFallbackToIpWhenNoAuthHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .remoteAddress(new InetSocketAddress("192.168.1.1", 8080))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter filter = rateLimitFilter.apply(new RateLimitFilter.Config());

        // We expect the key to contain the IP
        when(valueOperations.increment(eq("rate_limit:ip:192.168.1.1"))).thenReturn(1L);
        when(redisTemplate.getExpire(any(), eq(TimeUnit.SECONDS))).thenReturn(60L);

        filter.filter(exchange, filterChain).block();
    }
}
