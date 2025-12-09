package com.yanis.api_gateway.exception;

/**
 * Exception thrown when rate limit is exceeded.
 *
 * <p>
 * Used by RateLimitFilter when a user exceeds the allowed
 * number of requests per time window.
 * </p>
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }

    public RateLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
