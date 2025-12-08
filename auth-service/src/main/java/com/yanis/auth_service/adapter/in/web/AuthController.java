package com.yanis.auth_service.adapter.in.web;

import org.springframework.http.HttpStatus;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for authentication endpoints.
 *
 * <p>
 * Provides endpoints for user registration and login.
 * All endpoints are publicly accessible (no authentication required).
 * </p>
 *
 * <p>
 * <strong>Base Path:</strong> {@code /auth}
 * </p>
 *
 * @see RegisterUserUseCase
 * @see LoginUserUseCase
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {

        private final RegisterUserUseCase registerUserUseCase;
        private final LoginUserUseCase loginUserUseCase;

        @Operation(summary = "Register a new user", description = "Creates a new user account with email and password. Returns JWT tokens upon successful registration.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "User successfully registered", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request (validation errors)", content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
                        @ApiResponse(responseCode = "409", description = "User with this email already exists", content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
        })
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

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(new AuthResponse(
                                                tokens.accessToken(),
                                                tokens.refreshToken()));
        }

        @Operation(summary = "Authenticate user", description = "Authenticates a user with email and password. Returns JWT tokens upon successful login.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "User successfully authenticated", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request (validation errors)", content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
                        @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
        })
        @PostMapping("/login")
        public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
                log.info("Login request received for email: {}", request.email());

                LoginUserUseCase.AuthTokens tokens = loginUserUseCase.login(
                                request.email(),
                                request.password());

                return ResponseEntity.ok(new AuthResponse(
                                tokens.accessToken(),
                                tokens.refreshToken()));
        }

        @Operation(summary = "Health check", description = "Simple health check endpoint to verify the service is running.")
        @ApiResponse(responseCode = "200", description = "Service is healthy")
        @GetMapping("/health")
        public ResponseEntity<String> health() {
                return ResponseEntity.ok("Auth Service is running");
        }
}
