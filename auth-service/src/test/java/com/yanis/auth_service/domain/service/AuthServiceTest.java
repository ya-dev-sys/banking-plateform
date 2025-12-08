package com.yanis.auth_service.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.yanis.auth_service.domain.exception.InvalidCredentialsException;
import com.yanis.auth_service.domain.exception.UserAlreadyExistsException;
import com.yanis.auth_service.domain.model.User;
import com.yanis.auth_service.domain.port.in.LoginUserUseCase.AuthTokens;
import com.yanis.auth_service.domain.port.out.UserRepository;

/**
 * Unit tests for AuthService.
 *
 * <p>
 * Tests business logic in isolation using Mockito mocks.
 * No Spring context loaded - fast execution (< 100ms per test).
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("$2a$10$hashedPassword")
                .createdAt(LocalDateTime.now())
                .build();
        testUser.addRole("USER");
    }

    // ========== REGISTRATION TESTS ==========

    @Test
    @DisplayName("register() - Success - Returns user with hashed password")
    void register_Success_ReturnsUserWithHashedPassword() {
        // Arrange
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return User.builder()
                    .id(1L)
                    .email(user.getEmail())
                    .passwordHash(user.getPasswordHash())
                    .createdAt(user.getCreatedAt())
                    .build();
        });

        // Act
        User result = authService.register("test@example.com", "password123", "John", "Doe");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getPasswordHash()).isEqualTo("$2a$10$hashedPassword");
        assertThat(result.getId()).isEqualTo(1L);

        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register() - Duplicate email - Throws UserAlreadyExistsException")
    void register_DuplicateEmail_ThrowsUserAlreadyExistsException() {
        // Arrange
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register("test@example.com", "password123", "John", "Doe"))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("test@example.com");

        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("register() - Success - Assigns USER role")
    void register_AssignsUserRole() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return User.builder()
                    .id(1L)
                    .email(user.getEmail())
                    .passwordHash(user.getPasswordHash())
                    .createdAt(user.getCreatedAt())
                    .build();
        });

        authService.register("test@example.com", "password123", "John", "Doe");

        // Assert
        verify(userRepository).save(argThat(user -> user.getRoles().contains("USER")));
    }

    // ========== LOGIN TESTS ==========

    @Test
    @DisplayName("login() - Valid credentials - Returns AuthTokens")
    void login_ValidCredentials_ReturnsAuthTokens() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtService.generateAccessToken(testUser)).thenReturn("access-token-123");
        when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh-token-456");

        // Act
        AuthTokens result = authService.login("test@example.com", "password123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("access-token-123");
        assertThat(result.refreshToken()).isEqualTo("refresh-token-456");

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("password123", "$2a$10$hashedPassword");
        verify(jwtService).generateAccessToken(testUser);
        verify(jwtService).generateRefreshToken(testUser);
    }

    @Test
    @DisplayName("login() - Invalid email - Throws InvalidCredentialsException")
    void login_InvalidEmail_ThrowsInvalidCredentialsException() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login("nonexistent@example.com", "password123"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("login() - Invalid password - Throws InvalidCredentialsException")
    void login_InvalidPassword_ThrowsInvalidCredentialsException() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "$2a$10$hashedPassword")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login("test@example.com", "wrongpassword"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("wrongpassword", "$2a$10$hashedPassword");
        verify(jwtService, never()).generateAccessToken(any());
    }
}
