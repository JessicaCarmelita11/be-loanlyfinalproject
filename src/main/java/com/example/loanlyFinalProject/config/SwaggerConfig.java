package com.example.loanlyFinalProject.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "Loan Banking System API",
            version = "1.0.0",
            description = "REST API documentation for Loan Banking System",
            contact = @Contact(name = "Support Team", email = "support@loanbankingsystem.com"),
            license = @License(name = "MIT License", url = "https://opensource.org/licenses/MIT")))
@SecurityScheme(
    name = "Bearer Authentication",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer",
    description = "Enter JWT token")
public class SwaggerConfig {
  // Using annotation-based configuration instead of bean-based
}
