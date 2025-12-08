package com.yanis.auth_service.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user authentication.
 *
 * @param email    User's email address.
 * @param password User's password in plain text (will be verified against
 *                 BCrypt hash).
 */
public record LoginRequest(
                @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,

                @NotBlank(message = "Password is required") String password) {
}
