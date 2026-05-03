package com.eshop.app.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for OpenAPI documentation.
 */
@Data
@Validated
@ConfigurationProperties(prefix = "app.openapi")
public class OpenApiProperties {

    /**
     * Enable/disable OpenAPI documentation.
     */
    private boolean enabled = false;

    /**
     * API title displayed in documentation.
     */
    private String title = "E-Shop API";

    /**
     * API version.
     */
    private String version = "1.0.0";

    /**
     * API description (supports Markdown).
     */
    private String description = "E-Commerce Platform REST API";

    /**
     * URL to external documentation.
     */
    private String externalDocsUrl;

    /**
     * Contact information.
     */
    @Valid
    private ContactInfo contact = new ContactInfo();

    /**
     * License information.
     */
    @Valid
    private LicenseInfo license = new LicenseInfo();

    /**
     * Server configurations.
     */
    private List<@Valid ServerInfo> servers = new ArrayList<>();

    /**
     * API tags for grouping endpoints.
     */
    private List<@Valid TagInfo> tags = new ArrayList<>();

    @Data
    public static class ContactInfo {
        private String name = "E-Shop Support";
        private String email = "support@eshop.com";
        private String url = "https://eshop.com/support";
    }

    @Data
    public static class LicenseInfo {
        private String name = "Apache 2.0";
        private String url = "https://www.apache.org/licenses/LICENSE-2.0";
    }

    @Data
    public static class ServerInfo {
        @NotBlank(message = "Server URL is required")
        private String url;
        private String description;
    }

    @Data
    public static class TagInfo {
        @NotBlank(message = "Tag name is required")
        private String name;
        private String description;
    }
}