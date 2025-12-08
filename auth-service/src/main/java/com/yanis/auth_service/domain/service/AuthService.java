package com.yanis.auth_service.domain.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yanis.auth_service.domain.exception.InvalidCredentialsException;
import com.yanis.auth_service.domain.exception.UserAlreadyExistsException;
import com.yanis.auth_service.domain.model.User;
import com.yanis.auth_service.domain.port.in.LoginUserUseCase;
import com.yanis.auth_service.domain.port.in.RegisterUserUseCase;
import com.yanis.auth_service.domain.port.out.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Core authentication service implementing user registration and login.
 *
 * <p>
 * This service handles:
 * <ul>
 * <li>User registration with email uniqueness validation</li>
 * <li>Password hashing using BCrypt</li>
 * <li>User authentication with credential verification</li>
 * <li>JWT token generation for authenticated users</li>
 * </ul>
 *
 * @see RegisterUserUseCase
 * @see LoginUserUseCase
 * @see JwtService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements RegisterUserUseCase, LoginUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public User register(String email, String password, String firstName, String lastName) {
        log.info("Attempting to register user with email: {}", email);

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(email);
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .createdAt(LocalDateTime.now())
                .build();

        user.addRole("USER");

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with id: {}", savedUser.getId());

        return savedUser;
    }

    @Override
    public AuthTokens login(String email, String password) {
        log.info("Attempting login for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("User logged in successfully: {}", email);

        return new AuthTokens(accessToken, refreshToken);
    }
}
