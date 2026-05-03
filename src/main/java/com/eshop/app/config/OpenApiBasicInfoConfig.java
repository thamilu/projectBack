package com.eshop.app.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "E-Shop REST API",
        version = "1.0.0",
        description = "Backend APIs for E-Commerce platform",
        contact = @Contact(
            name = "E-Shop Team",
            email = "support@eshop.com"
        )
    )
)
public class OpenApiBasicInfoConfig {
}
