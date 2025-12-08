package com.yanis.auth_service.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yanis.auth_service.adapter.in.web.dto.AuthResponse;
import com.yanis.auth_service.adapter.in.web.dto.LoginRequest;
import com.yanis.auth_service.adapter.in.web.dto.RegisterRequest;
import com.yanis.auth_service.domain.port.in.LoginUserUseCase;
import com.yanis.auth_service.domain.port.in.RegisterUserUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

        private final RegisterUserUseCase registerUserUseCase;
        private final LoginUserUseCase loginUserUseCase;

        @PostMapping("/register")
        public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
                log.info("Registration request received for email: {}", request.email());

                registerUserUseCase.register(
                                request.email(),
                                request.password(),
                                request.firstName(),
                                request.lastName());

                LoginUserUseCase.AuthTokens tokens = loginUserUseCase.login(
                                request.email(),
                                request.password());

                return ResponseEntity.ok(AuthResponse.builder()
                                .accessToken(tokens.accessToken())
                                .refreshToken(tokens.refreshToken())
                                .build());
        }

        @PostMapping("/login")
        public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
                log.info("Login request received for email: {}", request.email());

                LoginUserUseCase.AuthTokens tokens = loginUserUseCase.login(
                                request.email(),
                                request.password());

                return ResponseEntity.ok(AuthResponse.builder()
                                .accessToken(tokens.accessToken())
                                .refreshToken(tokens.refreshToken())
                                .build());
        }

        @GetMapping("/health")
        public ResponseEntity<String> health() {
                return ResponseEntity.ok("Auth Service is running");
        }
}
