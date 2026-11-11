package com.virtualclassroom.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Virtual Classroom Platform API",
                version = "1.0.0",
                description = "Enterprise Modular Monolith REST API for Academic Management, Curriculum Delivery, and Virtual Classroom Operations.",
                contact = @Contact(
                        name = "Platform Engineering Team",
                        email = "support@virtualclassroom.edu"
                ),
                license = @License(
                        name = "Academic & Enterprise License",
                        url = "https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform"
                )
        ),
        security = @SecurityRequirement(name = "BearerAuth")
)
@SecurityScheme(
        name = "BearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Enter JWT access token issued by the /api/v1/auth/login endpoint."
)
public class OpenApiConfig {
}
