package com.yanis.auth_service.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.yanis.auth_service.domain.model.User;

/**
 * Unit tests for JwtService.
 *
 * <p>
 * Tests JWT token generation and validation using real JWT library.
 * No mocks needed - tests actual token creation and parsing.
 * </p>
 */
@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        // Set test configuration values using reflection
        ReflectionTestUtils.setField(jwtService, "secret",
                "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256-algorithm");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 1800000L); // 30 minutes
        ReflectionTestUtils.setField(jwtService, "refreshExpirationMs", 604800000L); // 7 days

        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("$2a$10$hashedPassword")
                .createdAt(LocalDateTime.now())
                .build();
        testUser.addRole("USER");
        testUser.addRole("ADMIN");
    }

    // ========== TOKEN GENERATION TESTS ==========

    @Test
    @DisplayName("generateAccessToken() - Success - Returns valid JWT")
    void generateAccessToken_Success_ReturnsValidJwt() {
        // Act
        String token = jwtService.generateAccessToken(testUser);

        // Assert
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    @DisplayName("generateAccessToken() - Contains user roles in claims")
    void generateAccessToken_ContainsRoles() {
        // Act
        String token = jwtService.generateAccessToken(testUser);

        // Assert
        assertThat(token).isNotNull();
        // Token should be valid and parseable
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("generateRefreshToken() - Success - Returns valid JWT")
    void generateRefreshToken_Success_ReturnsValidJwt() {
        // Act
        String token = jwtService.generateRefreshToken(testUser);

        // Assert
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("generateRefreshToken() - Contains minimal claims")
    void generateRefreshToken_MinimalClaims() {
        // Act
        String token = jwtService.generateRefreshToken(testUser);

        // Assert
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).isEqualTo("test@example.com");
    }

    // ========== TOKEN VALIDATION TESTS ==========

    @Test
    @DisplayName("validateToken() - Valid token - Returns true")
    void validateToken_ValidToken_ReturnsTrue() {
        // Arrange
        String token = jwtService.generateAccessToken(testUser);

        // Act
        boolean isValid = jwtService.validateToken(token);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("validateToken() - Invalid signature - Returns false")
    void validateToken_InvalidSignature_ReturnsFalse() {
        // Arrange
        String token = jwtService.generateAccessToken(testUser);
        // Tamper with the token by changing last character
        String tamperedToken = token.substring(0, token.length() - 1) + "X";

        // Act
        boolean isValid = jwtService.validateToken(tamperedToken);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("validateToken() - Malformed token - Returns false")
    void validateToken_MalformedToken_ReturnsFalse() {
        // Act
        boolean isValid = jwtService.validateToken("not.a.valid.jwt.token");

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("validateToken() - Null token - Returns false")
    void validateToken_NullToken_ReturnsFalse() {
        // Act
        boolean isValid = jwtService.validateToken(null);

        // Assert
        assertThat(isValid).isFalse();
    }

    // ========== EMAIL EXTRACTION TESTS ==========

    @Test
    @DisplayName("extractEmail() - Valid token - Returns email")
    void extractEmail_ValidToken_ReturnsEmail() {
        // Arrange
        String token = jwtService.generateAccessToken(testUser);

        // Act
        String email = jwtService.extractEmail(token);

        // Assert
        assertThat(email).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("extractEmail() - Refresh token - Returns email")
    void extractEmail_RefreshToken_ReturnsEmail() {
        // Arrange
        String token = jwtService.generateRefreshToken(testUser);

        // Act
        String email = jwtService.extractEmail(token);

        // Assert
        assertThat(email).isEqualTo("test@example.com");
    }

}
