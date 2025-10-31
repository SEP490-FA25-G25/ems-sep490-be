package org.fyp.tmssep490be.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation
 *
 * Access Swagger UI at: http://localhost:8080/swagger-ui.html
 * Access API docs at: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenAPIConfiguration {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        // Define JWT security scheme
        final String securitySchemeName = "Bearer Authentication";

        return new OpenAPI()
                .info(new Info()
                        .title("Training Management System API")
                        .version("1.0.0")
                        .description("""
                                A comprehensive B2B SaaS platform for multi-branch language training centers.
                                This backend service manages the complete training lifecycle from curriculum design
                                through graduation, supporting offline, online, and hybrid learning modalities.

                                **Key Features:**
                                - Multi-branch center management
                                - Curriculum design with PLO/CLO mapping
                                - Class scheduling and resource allocation
                                - Student enrollment and attendance tracking
                                - Assessment and feedback management
                                - Teacher assignment and availability
                                - Request workflows (student/teacher requests)

                                **Authentication:**
                                All protected endpoints require a valid JWT token. Use the `/api/v1/auth/login`
                                endpoint to obtain a token, then click 'Authorize' button and enter: `Bearer <token>`
                                """)
                        .contact(new Contact()
                                .name("TMS Development Team")
                                .email("support@tms.example.com"))
                        .license(new License()
                                .name("Private License")
                                .url("https://tms.example.com/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Development server"),
                        new Server()
                                .url("https://api.tms.example.com")
                                .description("Production server (example)")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token obtained from /api/v1/auth/login endpoint")));
    }
}
