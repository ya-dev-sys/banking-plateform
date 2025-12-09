package com.yanis.api_gateway.controller;

import java.net.URI;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

/**
 * Controller to handle Circuit Breaker fallback requests.
 * Returns standard error responses when services are unavailable.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/auth")
    public Mono<ResponseEntity<ProblemDetail>> authFallback() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Auth Service is currently unavailable. Please try again later.");
        problemDetail.setTitle("Service Unavailable");
        problemDetail.setInstance(URI.create("/api/auth"));
        problemDetail.setProperty("timestamp", Instant.now());

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problemDetail));
    }

    @RequestMapping("/account")
    public Mono<ResponseEntity<ProblemDetail>> accountFallback() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Account Service is currently unavailable. Please try again later.");
        problemDetail.setTitle("Service Unavailable");
        problemDetail.setInstance(URI.create("/api/accounts"));
        problemDetail.setProperty("timestamp", Instant.now());

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problemDetail));
    }

    @RequestMapping("/user")
    public Mono<ResponseEntity<ProblemDetail>> userFallback() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "User Service is currently unavailable. Please try again later.");
        problemDetail.setTitle("Service Unavailable");
        problemDetail.setInstance(URI.create("/api/users"));
        problemDetail.setProperty("timestamp", Instant.now());

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problemDetail));
    }
}
