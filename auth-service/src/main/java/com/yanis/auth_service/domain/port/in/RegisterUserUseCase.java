package com.yanis.auth_service.domain.port.in;

import com.yanis.auth_service.domain.model.User;

/**
 * Use case for registering new users in the system.
 *
 * <p>
 * This interface defines the contract for user registration,
 * which includes email uniqueness validation and password hashing.
 * </p>
 *
 * @see com.yanis.auth_service.domain.service.AuthService
 */
public interface RegisterUserUseCase {

    /**
     * Registers a new user with the provided credentials.
     *
     * <p>
     * The password will be hashed using BCrypt before storage.
     * The user will be assigned the default "USER" role.
     * </p>
     *
     * @param email     The user's email address (must be unique).
     * @param password  The user's password in plain text (min 8 characters).
     * @param firstName The user's first name.
     * @param lastName  The user's last name.
     * @return The created user with generated ID and hashed password.
     * @throws com.yanis.auth_service.domain.exception.UserAlreadyExistsException
     *                                                                            if
     *                                                                            email
     *                                                                            is
     *                                                                            already
     *                                                                            registered.
     */
    User register(String email, String password, String firstName, String lastName);
}
