package com.yanis.api_gateway.filter;

import java.time.Duration;
import java.time.Instant;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Logging filter for request/response tracking.
 *
 * <p>
 * Logs all incoming requests and outgoing responses with:
 * <ul>
 * <li>HTTP method and path</li>
 * <li>Response status code</li>
 * <li>Request duration (latency)</li>
 * <li>Trace ID for distributed tracing</li>
 * </ul>
 */
@Component
@Slf4j
public class LoggingFilter extends AbstractGatewayFilterFactory<LoggingFilter.Config> {

    public LoggingFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            Instant startTime = Instant.now();

            String method = exchange.getRequest().getMethod().toString();
            String path = exchange.getRequest().getPath().toString();
            String traceId = exchange.getRequest().getId();

            log.info(">>> Incoming request: {} {} [traceId: {}]", method, path, traceId);

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                Instant endTime = Instant.now();
                Duration duration = Duration.between(startTime, endTime);

                int statusCode = exchange.getResponse().getStatusCode() != null
                        ? exchange.getResponse().getStatusCode().value()
                        : 0;

                log.info("<<< Response: {} {} - Status: {} - Duration: {}ms [traceId: {}]",
                        method, path, statusCode, duration.toMillis(), traceId);
            }));
        };
    }

    /**
     * Configuration class for the filter.
     */
    public static class Config {
        // Configuration properties if needed
    }
}
