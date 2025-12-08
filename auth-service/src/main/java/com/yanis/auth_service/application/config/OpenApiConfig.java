package com.yanis.auth_service.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

/**
 * OpenAPI/Swagger configuration for Auth Service API documentation.
 *
 * <p>
 * Provides interactive API documentation accessible at:
 * <ul>
 * <li>Swagger UI: http://localhost:8081/swagger-ui.html</li>
 * <li>OpenAPI JSON: http://localhost:8081/v3/api-docs</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configures OpenAPI documentation metadata.
     *
     * @return OpenAPI configuration with service information.
     */
    @Bean
    public OpenAPI authServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Service API")
                        .description("Authentication and Authorization service for the Banking Platform. " +
                                "Provides user registration, login, and JWT token management.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Banking Platform Team")
                                .email("support@banking-platform.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addServersItem(new Server()
                        .url("http://localhost:8081")
                        .description("Local Development Server"))
                .addServersItem(new Server()
                        .url("http://localhost:8080")
                        .description("API Gateway (Production)"));
    }
}
