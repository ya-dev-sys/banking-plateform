package com.yanis.api_gateway.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JWT Token Provider for API Gateway.
 *
 * <p>
 * Validates JWT tokens issued by Auth Service and extracts user information.
 * Uses the same secret key as Auth Service to verify token signatures.
 * </p>
 *
 * <p>
 * <strong>Security Note:</strong> The JWT secret must match the Auth Service
 * secret
 * for proper token validation.
 * </p>
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret}")
    private String secret;

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
            logger.error("Invalid JWT token: {}", e.getMessage());
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

    /**
     * Extracts user roles from a JWT token.
     *
     * @param token The JWT token.
     * @return List of user roles, or empty list if no roles found.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = extractClaims(token);
        Object roles = claims.get("roles");

        if (roles instanceof List) {
            return (List<String>) roles;
        }

        return List.of();
    }

    /**
     * Checks if a token is expired.
     *
     * @param token The JWT token.
     * @return true if token is expired, false otherwise.
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Extracts all claims from a JWT token.
     *
     * @param token The JWT token.
     * @return Claims object containing all token claims.
     */
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Gets the signing key for JWT verification.
     *
     * @return SecretKey for HMAC-SHA256 signature verification.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
