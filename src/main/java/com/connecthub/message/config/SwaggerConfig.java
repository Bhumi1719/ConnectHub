package com.connecthub.message.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** Swagger UI: http://localhost:8083/swagger-ui/index.html */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Chat Application API — Message Service")
                        .version("1.0")
                        .description("Send, edit, delete messages. Update delivery and read status.")
                        .contact(new Contact().name("ConnectHub Team").email("dev@connecthub.com"))
                )
                .servers(List.of(
                        new Server().url("http://localhost:8083").description("Message Service — Direct"),
                        new Server().url("http://localhost:8080").description("API Gateway (/messages/**)")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste JWT from POST /auth/login")
                        )
                );
    }
}
