package com.connecthub.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 / Swagger configuration for Auth Service.
 *
 * Swagger UI: http://localhost:8081/swagger-ui/index.html
 * API Docs:   http://localhost:8081/v3/api-docs
 */
@Configuration
public class SwaggerConfig {

    private static final String BEARER_AUTH_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI authServiceOpenAPI() {
        return new OpenAPI()
                .info(buildApiInfo())
                .servers(buildServers())
                // ✅ JWT Bearer token security requirement applied globally
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH_SCHEME, buildBearerSecurityScheme())
                );
    }

    // ─── Info ─────────────────────────────────────────────────────────────────

    private Info buildApiInfo() {
        return new Info()
                .title("Chat Application API — Auth Service")
                .version("1.0")
                .description("""
                        ## ConnectHub Auth Service
                        
                        Handles user **registration**, **login**, **JWT token** issuance,
                        **Google OAuth2** login, profile management, and token validation.
                        
                        ### Authentication
                        - **Local Login**: `POST /auth/login` → returns a Bearer token.
                        - **Google OAuth2**: `GET /auth/oauth2/authorize/google` → browser redirect flow → returns JWT.
                        - Paste the token into the **Authorize** button (top right) to test secured endpoints.
                        
                        ### Services
                        | Service          | Port |
                        |-----------------|------|
                        | Auth Service     | 8081 |
                        | Room Service     | 8082 |
                        | Message Service  | 8083 |
                        | Presence Service | 8084 |
                        | Notification     | 8085 |
                        | Media Service    | 8086 |
                        | WebSocket        | 8087 |
                        | API Gateway      | 8080 |
                        """)
                .contact(new Contact()
                        .name("ConnectHub Team")
                        .email("dev@connecthub.com")
                        .url("https://github.com/connecthub"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    // ─── Servers ──────────────────────────────────────────────────────────────

    private List<Server> buildServers() {
        return List.of(
                new Server()
                        .url("http://localhost:8081")
                        .description("Auth Service — Direct"),
                new Server()
                        .url("http://localhost:8080")
                        .description("API Gateway (routes /auth/** here)")
        );
    }

    // ─── JWT Bearer Security Scheme ───────────────────────────────────────────

    private SecurityScheme buildBearerSecurityScheme() {
        return new SecurityScheme()
                .name(BEARER_AUTH_SCHEME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        Enter your JWT token obtained from `POST /auth/login` or Google OAuth2 login.
                        
                        **Format**: `Bearer <your_token_here>`
                        
                        The Authorize dialog will add the `Bearer ` prefix automatically.
                        """);
    }
}
