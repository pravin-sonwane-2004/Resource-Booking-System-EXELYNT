package com.pravin.Resource_Booking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration. Swagger UI is available at /swagger-ui.html.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI resourceBookingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Resource Booking API")
                        .description("""
                                RESTful resource booking system built with Spring Boot, Spring Security and JWT.

                                Security:
                                - Authenticate via `POST /auth/login` and copy the returned `token`.
                                - Click the **Authorize** button below and paste it as `Bearer <token>`.
                                - ADMIN role can manage everything; USER role can read resources and manage their own reservations.
                                """)
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}