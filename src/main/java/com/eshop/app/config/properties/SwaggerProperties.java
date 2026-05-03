package com.eshop.app.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
// Note: servers list may be empty in some deployments (e.g., behind proxies). No NotEmpty validation.
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * Externalized Swagger/OpenAPI Configuration Properties
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.swagger")
public class SwaggerProperties {

    /** Enable/disable Swagger UI */
    private boolean enabled = true;

    /** API information */
    @Valid
    private ApiInfo apiInfo = new ApiInfo();

    /** Server configurations */
    @Valid
    private List<ServerConfig> servers = new ArrayList<>();

    /** Security configuration */
    @Valid
    private Security security = new Security();

    /** API groups */
    private List<ApiGroup> groups = new ArrayList<>();

    @Getter
    @Setter
    public static class ApiInfo {
        @NotBlank(message = "API title is required")
        private String title = "EShop API";
        private String description = "E-commerce Platform API";
        @NotBlank(message = "API version is required")
        private String version = "4.0.0";
        @Valid
        private Contact contact = new Contact();
        @Valid
        private LicenseInfo license = new LicenseInfo();
        private String termsOfServiceUrl;
    }

    @Getter
    @Setter
    public static class Contact {
        private String name = "EShop Development Team";
        @Email(message = "Invalid contact email")
        private String email = "dev@eshop.com";
        private String url = "https://eshop.com";
    }

    @Getter
    @Setter
    public static class LicenseInfo {
        private String name = "MIT License";
        private String url = "https://opensource.org/licenses/MIT";
    }

    @Getter
    @Setter
    public static class ServerConfig {
        @NotBlank(message = "Server URL is required")
        private String url;
        @NotBlank(message = "Server description is required")
        private String description;
        private String environment = "dev";
    }

    @Getter
    @Setter
    public static class Security {
        private boolean bearerEnabled = true;
        private boolean oauth2Enabled = true;
        private String authorizationUrl;
        private String tokenUrl;
        private List<String> scopes = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class ApiGroup {
        @NotBlank
        private String name;
        private String displayName;
        private List<String> pathPatterns = new ArrayList<>();
    }
}
