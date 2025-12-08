package com.yanis.auth_service.domain.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.yanis.auth_service.domain.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for JWT token generation and validation.
 *
 * <p>
 * Uses JJWT library with HMAC-SHA256 signing algorithm.
 * Tokens include user email as subject and roles as claims.
 * </p>
 *
 * <p>
 * <strong>Security Note:</strong> The JWT secret must be at least 256 bits
 * and should be stored securely (environment variable or secrets manager).
 * </p>
 *
 * <p>
 * Token Lifetimes:
 * <ul>
 * <li>Access Token: 30 minutes (for API access)</li>
 * <li>Refresh Token: 7 days (for obtaining new access tokens)</li>
 * </ul>
 *
 * @see io.jsonwebtoken.Jwts
 * @see com.yanis.auth_service.domain.model.User
 */
@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    /**
     * Generates a short-lived access token for API authentication.
     *
     * <p>
     * Includes user roles as claims for authorization purposes.
     * </p>
     *
     * @param user The user for whom to generate the token.
     * @return JWT access token valid for 30 minutes.
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles());
        return generateToken(claims, user.getEmail(), expirationMs);
    }

    /**
     * Generates a long-lived refresh token for obtaining new access tokens.
     *
     * <p>
     * Contains minimal claims (only subject/email) for security.
     * </p>
     *
     * @param user The user for whom to generate the token.
     * @return JWT refresh token valid for 7 days.
     */
    public String generateRefreshToken(User user) {
        return generateToken(new HashMap<>(), user.getEmail(), refreshExpirationMs);
    }

    private String generateToken(Map<String, Object> claims, String subject, long expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Validates a JWT token's signature and expiration.
     *
     * @param token The JWT token to validate.
     * @return true if token is valid and not expired, false otherwise.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extracts the user email from a JWT token.
     *
     * @param token The JWT token.
     * @return The email address stored in the token's subject claim.
     */
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
