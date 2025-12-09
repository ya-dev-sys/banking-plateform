package com.yanis.api_gateway.exception;

/**
 * Exception thrown when JWT authentication fails.
 *
 * <p>
 * Used by AuthenticationFilter when:
 * <ul>
 * <li>Authorization header is missing</li>
 * <li>Token format is invalid</li>
 * <li>Token signature is invalid</li>
 * <li>Token is expired</li>
 * </ul>
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
