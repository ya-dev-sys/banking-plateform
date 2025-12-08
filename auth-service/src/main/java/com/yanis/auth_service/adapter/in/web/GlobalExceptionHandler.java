package com.yanis.auth_service.adapter.in.web;

import java.net.URI;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.yanis.auth_service.domain.exception.InvalidCredentialsException;
import com.yanis.auth_service.domain.exception.UserAlreadyExistsException;

import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for Auth Service.
 *
 * <p>
 * Converts domain exceptions to RFC 7807 ProblemDetail responses
 * with appropriate HTTP status codes.
 * </p>
 *
 * @see ProblemDetail
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String TIMESTAMP_PROPERTY = "timestamp";

    /**
     * Handles user already exists exception (email conflict).
     *
     * @param ex The exception thrown when attempting to register duplicate email.
     * @return ProblemDetail with 409 Conflict status.
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.warn("User registration conflict: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage());
        problem.setTitle("User Already Exists");
        problem.setType(URI.create("/errors/user-already-exists"));
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());

        return problem;
    }

    /**
     * Handles invalid credentials exception (login failure).
     *
     * @param ex The exception thrown when credentials don't match.
     * @return ProblemDetail with 401 Unauthorized status.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password");
        problem.setTitle("Authentication Failed");
        problem.setType(URI.create("/errors/invalid-credentials"));
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());

        return problem;
    }

    /**
     * Handles validation errors from @Valid annotations.
     *
     * @param ex The exception containing field validation errors.
     * @return ProblemDetail with 400 Bad Request status and field errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {} errors", ex.getErrorCount());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed for one or more fields");
        problem.setTitle("Validation Error");
        problem.setType(URI.create("/errors/validation-failed"));
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());

        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        problem.setProperty("errors", errors);

        return problem;
    }
}
