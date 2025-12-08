package com.yanis.auth_service.domain.port.in;

import com.yanis.auth_service.domain.model.User;

public interface RegisterUserUseCase {
    User register(String email, String password, String firstName, String lastName);
}
