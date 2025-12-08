package com.yanis.auth_service.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for user registration.
 *
 * @param email     User's email address (must be valid and unique).
 * @param password  User's password (min 8 characters, will be hashed with
 *                  BCrypt).
 * @param firstName User's first name.
 * @param lastName  User's last name.
 */
public record RegisterRequest(
                @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,

                @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters") String password,

                @NotBlank(message = "First name is required") String firstName,

                @NotBlank(message = "Last name is required") String lastName) {
}
