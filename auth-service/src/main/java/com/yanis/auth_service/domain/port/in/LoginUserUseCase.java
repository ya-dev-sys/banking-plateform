package com.yanis.auth_service.domain.port.in;

public interface LoginUserUseCase {
    AuthTokens login(String email, String password);

    record AuthTokens(String accessToken, String refreshToken) {}
}
