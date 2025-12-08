package com.yanis.auth_service.adapter.in.web.dto;

/**
 * Response DTO containing JWT authentication tokens.
 *
 * @param accessToken  Short-lived JWT for API access (30 minutes).
 * @param refreshToken Long-lived JWT for obtaining new access tokens (7 days).
 * @param tokenType    Token type (always "Bearer" for JWT).
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType) {
    /**
     * Creates an AuthResponse with Bearer token type.
     *
     * @param accessToken  The JWT access token.
     * @param refreshToken The JWT refresh token.
     */
    public AuthResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, "Bearer");
    }
}
