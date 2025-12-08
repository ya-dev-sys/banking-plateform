package com.yanis.auth_service.domain.port.in;

/**
 * Use case for authenticating users and generating JWT tokens.
 *
 * <p>
 * This interface defines the contract for user login,
 * which includes credential verification and JWT token generation.
 * </p>
 *
 * @see com.yanis.auth_service.domain.service.AuthService
 * @see com.yanis.auth_service.domain.service.JwtService
 */
public interface LoginUserUseCase {

    /**
     * Authenticates a user and returns JWT tokens.
     *
     * <p>
     * Verifies the password against the stored BCrypt hash.
     * On success, generates both access and refresh tokens.
     * </p>
     *
     * @param email    The user's email address.
     * @param password The user's password in plain text.
     * @return AuthTokens containing access and refresh JWT tokens.
     * @throws com.yanis.auth_service.domain.exception.InvalidCredentialsException
     *                                                                             if
     *                                                                             if
     *                                                                             email
     *                                                                             doesn't
     *                                                                             exist
     *                                                                             or
     *                                                                             password
     *                                                                             doesn't
     *                                                                             match.
     */
    AuthTokens login(String email, String password);

    /**
     * Container for JWT authentication tokens.
     *
     * @param accessToken  Short-lived token for API access (30 min).
     * @param refreshToken Long-lived token for renewing access (7 days).
     */
    record AuthTokens(String accessToken, String refreshToken) {
    }
}
