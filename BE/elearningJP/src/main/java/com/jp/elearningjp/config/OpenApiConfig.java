package com.jp.elearningjp.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "E-Learning JP REST API",
                version = "v1",
                description = "Backend API for the E-Learning JP platform — " +
                        "courses, lessons, enrollments, progress, auth, payments, " +
                        "community, gamification, etc.",
                contact = @Contact(name = "E-Learning JP Team"),
                license = @License(name = "Proprietary")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local dev"),
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        in = SecuritySchemeIn.HEADER,
        description = "Paste the JWT access token here (without the 'Bearer ' prefix)."
)
public class OpenApiConfig {
}
