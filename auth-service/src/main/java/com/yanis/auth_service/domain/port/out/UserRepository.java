package com.yanis.auth_service.domain.port.out;

import java.util.Optional;

import com.yanis.auth_service.domain.model.User;

public interface UserRepository {
    User save(User user);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
