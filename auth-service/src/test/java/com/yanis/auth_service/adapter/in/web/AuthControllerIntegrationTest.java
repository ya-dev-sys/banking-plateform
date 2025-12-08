package com.yanis.auth_service.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for AuthController.
 *
 * <p>
 * Tests HTTP endpoints with full Spring context and real PostgreSQL database.
 * Uses Testcontainers for database isolation.
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test_db")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Database is automatically cleaned between tests due to @Transactional

    }

    // ========== REGISTRATION TESTS ==========

    @Test
    @DisplayName("POST /auth/register - Valid request - Returns 201 Created with tokens")
    void register_ValidRequest_Returns201Created() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "test@example.com",
                            "password": "password123",
                            "firstName": "John",
                            "lastName": "Doe"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /auth/register - Duplicate email - Returns 409 Conflict")
    void register_DuplicateEmail_Returns409Conflict() throws Exception {
        // First registration
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "duplicate@example.com",
                            "password": "password123",
                            "firstName": "John",
                            "lastName": "Doe"
                        }
                        """))
                .andExpect(status().isCreated());

        // Second registration with same email
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "duplicate@example.com",
                            "password": "password456",
                            "firstName": "Jane",
                            "lastName": "Smith"
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("User Already Exists"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.type").value("/errors/user-already-exists"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("POST /auth/register - Invalid email - Returns 400 Bad Request")
    void register_InvalidEmail_Returns400BadRequest() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "invalid-email",
                            "password": "password123",
                            "firstName": "John",
                            "lastName": "Doe"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /auth/register - Short password - Returns 400 Bad Request")
    void register_ShortPassword_Returns400BadRequest() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "test@example.com",
                            "password": "short",
                            "firstName": "John",
                            "lastName": "Doe"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /auth/register - Missing fields - Returns 400 Bad Request")
    void register_MissingFields_Returns400BadRequest() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "test@example.com"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    // ========== LOGIN TESTS ==========

    @Test
    @DisplayName("POST /auth/login - Valid credentials - Returns 200 with tokens")
    void login_ValidCredentials_Returns200WithTokens() throws Exception {
        // First register a user
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "login@example.com",
                            "password": "password123",
                            "firstName": "John",
                            "lastName": "Doe"
                        }
                        """))
                .andExpect(status().isCreated());

        // Then login
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "login@example.com",
                            "password": "password123"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /auth/login - Invalid credentials - Returns 401 Unauthorized")
    void login_InvalidCredentials_Returns401Unauthorized() throws Exception {
        // Register a user
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "user@example.com",
                            "password": "password123",
                            "firstName": "John",
                            "lastName": "Doe"
                        }
                        """))
                .andExpect(status().isCreated());

        // Try login with wrong password
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "user@example.com",
                            "password": "wrongpassword"
                        }
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Authentication Failed"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.type").value("/errors/invalid-credentials"));
    }

    @Test
    @DisplayName("POST /auth/login - Invalid email format - Returns 400 Bad Request")
    void login_InvalidEmail_Returns400BadRequest() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "not-an-email",
                            "password": "password123"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    // ========== HEALTH CHECK TEST ==========

    @Test
    @DisplayName("GET /auth/health - Returns 200")
    void health_Returns200() throws Exception {
        mockMvc.perform(get("/auth/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Auth Service is running"));
    }
}
