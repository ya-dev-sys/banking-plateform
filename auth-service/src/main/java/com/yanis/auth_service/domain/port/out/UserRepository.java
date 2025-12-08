package com.yanis.auth_service.domain.port.out;

import java.util.Optional;

import com.yanis.auth_service.domain.model.User;

/**
 * Repository port for User persistence operations.
 *
 * <p>
 * This interface defines the contract for user data access,
 * following the hexagonal architecture pattern (output port).
 * </p>
 *
 * @see com.yanis.auth_service.adapter.out.persistence.UserRepositoryImpl
 */
public interface UserRepository {

    /**
     * Saves a user to the database.
     *
     * @param user The user to save (with or without ID).
     * @return The saved user with generated ID if new.
     */
    User save(User user);

    /**
     * Finds a user by email address.
     *
     * @param email The email to search for.
     * @return Optional containing the user if found, empty otherwise.
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user with the given email exists.
     *
     * @param email The email to check.
     * @return true if user exists, false otherwise.
     */
    boolean existsByEmail(String email);
}
