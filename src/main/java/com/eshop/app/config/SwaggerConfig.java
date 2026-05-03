package com.eshop.app.config;

import com.eshop.app.config.properties.SwaggerProperties;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Swagger/OpenAPI Configuration (refactored)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.swagger", name = "enabled", havingValue = "true", matchIfMissing = true)
@SecuritySchemes({
        @SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, bearerFormat = "JWT", scheme = "bearer", description = "JWT Bearer token authentication"),
        @SecurityScheme(name = "oauth2", type = SecuritySchemeType.OAUTH2, description = "OAuth2 authentication with Keycloak",
                flows = @OAuthFlows(authorizationCode = @OAuthFlow(
                                                authorizationUrl = "${app.swagger.security.authorization-url}", tokenUrl = "${app.swagger.security.token-url}",
                        scopes = {@OAuthScope(name = "openid", description = "OpenID Connect scope"), @OAuthScope(name = "profile", description = "User profile"), @OAuthScope(name = "email", description = "User email")}
                ))),
        @SecurityScheme(name = "apiKey", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER, paramName = "X-API-KEY", description = "API Key for service-to-service communication")
})
public class SwaggerConfig implements WebMvcConfigurer {

        private static final String BEARER_AUTH = "bearerAuth";
        private static final String OAUTH2_AUTH = "oauth2";

        private final SwaggerProperties swaggerProperties;
        private final Environment environment;

        @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(OpenAPI.class)
        public OpenAPI openAPI() {
                log.info("Initializing OpenAPI documentation");
                try {
                        OpenAPI openAPI = new OpenAPI()
                                .info(buildApiInfo())
                                .servers(buildServers())
                                .tags(buildTags())
                                .components(buildComponents());

                        addSecurityRequirements(openAPI);
                        log.info("OpenAPI documentation initialized successfully for version: {}", swaggerProperties.getApiInfo().getVersion());
                        return openAPI;
                } catch (Exception e) {
                        log.error("Failed to initialize OpenAPI documentation: {}", e.getMessage(), e);
                        return buildFallbackOpenAPI();
                }
        }

        private Info buildApiInfo() {
                SwaggerProperties.ApiInfo apiInfo = swaggerProperties.getApiInfo();
                SwaggerProperties.Contact contactInfo = apiInfo.getContact();
                SwaggerProperties.LicenseInfo licenseInfo = apiInfo.getLicense();

                Info info = new Info()
                        .title(apiInfo.getTitle())
                        .description(buildDescription(apiInfo))
                        .version(apiInfo.getVersion());

                if (contactInfo != null && contactInfo.getName() != null) {
                        info.contact(new Contact().name(contactInfo.getName()).email(contactInfo.getEmail()).url(contactInfo.getUrl()));
                }

                if (licenseInfo != null && licenseInfo.getName() != null) {
                        info.license(new License().name(licenseInfo.getName()).url(licenseInfo.getUrl()));
                }

                if (apiInfo.getTermsOfServiceUrl() != null) {
                        info.termsOfService(apiInfo.getTermsOfServiceUrl());
                }

                return info;
        }

        private String buildDescription(SwaggerProperties.ApiInfo apiInfo) {
                String[] activeProfiles = environment.getActiveProfiles();
                String profileInfo = activeProfiles.length > 0 ? String.join(", ", activeProfiles) : "default";

                return String.format(
                        "%s\n\n---\n\n**Environment:** %s\n\n**Build Version:** %s\n\n**Java Version:** %s\n\n**Spring Boot Version:** %s\n\n---\n\n## Authentication\n\nThis API supports multiple authentication methods:\n\n1. **Bearer Token (JWT)** - For user authentication\n2. **OAuth2 (Keycloak)** - For SSO integration\n3. **API Key** - For service-to-service communication\n",
                        apiInfo.getDescription(),
                        profileInfo,
                        apiInfo.getVersion(),
                        System.getProperty("java.version"),
                        org.springframework.boot.SpringBootVersion.getVersion()
                );
        }

        private List<Server> buildServers() {
                String[] activeProfiles = environment.getActiveProfiles();
                boolean isProd = Arrays.asList(activeProfiles).contains("prod");

                return swaggerProperties.getServers().stream()
                        .filter(serverConfig -> {
                                if (isProd) return true;
                                return !"prod".equalsIgnoreCase(serverConfig.getEnvironment());
                        })
                        .map(this::buildServer)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
        }

        private Server buildServer(SwaggerProperties.ServerConfig serverConfig) {
                try {
                        return new Server().url(serverConfig.getUrl()).description(serverConfig.getDescription() + " [" + serverConfig.getEnvironment().toUpperCase() + "]");
                } catch (Exception e) {
                        log.warn("Failed to build server config for URL '{}': {}", serverConfig.getUrl(), e.getMessage());
                        return null;
                }
        }

        private List<Tag> buildTags() {
                return List.of(
                        new Tag().name("Authentication").description("User authentication and authorization endpoints"),
                        new Tag().name("Users Management").description("User management operations (Admin only)"),
                        new Tag().name("Products").description("Product catalog management"),
                        new Tag().name("Categories").description("Product category management"),
                        new Tag().name("Brands").description("Brand management"),
                        new Tag().name("Cart").description("Shopping cart operations"),
                        new Tag().name("Orders").description("Order management"),
                        new Tag().name("Shops").description("Shop/Seller management"),
                        new Tag().name("Delivery").description("Delivery and shipping operations"),
                        new Tag().name("Admin").description("Administrative operations"),
                        new Tag().name("Health").description("Application health and monitoring")
                );
        }

        private Components buildComponents() {
                Components components = new Components();

                components.addSchemas("ErrorResponse", new io.swagger.v3.oas.models.media.Schema<>().type("object").addProperty("timestamp", new io.swagger.v3.oas.models.media.Schema<>().type("string").format("date-time")).addProperty("status", new io.swagger.v3.oas.models.media.Schema<>().type("integer")).addProperty("error", new io.swagger.v3.oas.models.media.Schema<>().type("string")).addProperty("message", new io.swagger.v3.oas.models.media.Schema<>().type("string")).addProperty("path", new io.swagger.v3.oas.models.media.Schema<>().type("string")).addProperty("correlationId", new io.swagger.v3.oas.models.media.Schema<>().type("string")));

                components.addSchemas("PageMetadata", new io.swagger.v3.oas.models.media.Schema<>().type("object").addProperty("page", new io.swagger.v3.oas.models.media.Schema<>().type("integer")).addProperty("size", new io.swagger.v3.oas.models.media.Schema<>().type("integer")).addProperty("totalElements", new io.swagger.v3.oas.models.media.Schema<>().type("integer").format("int64")).addProperty("totalPages", new io.swagger.v3.oas.models.media.Schema<>().type("integer")));

                return components;
        }

        private void addSecurityRequirements(OpenAPI openAPI) {
                SwaggerProperties.Security security = swaggerProperties.getSecurity();
                if (security.isBearerEnabled()) {
                        openAPI.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
                }
                if (security.isOauth2Enabled()) {
                        openAPI.addSecurityItem(new SecurityRequirement().addList(OAUTH2_AUTH));
                }
        }

        @Override
        public void addViewControllers(ViewControllerRegistry registry) {
                List<String> redirectSources = List.of("/swagger-ui.html", "/swagger-ui", "/docs", "/api-docs",
                                "/documentation");
                redirectSources.forEach(source -> registry.addRedirectViewController(source, "/swagger-ui/index.html"));
                log.debug("Registered {} Swagger redirect view controllers", redirectSources.size());
        }

        private OpenAPI buildFallbackOpenAPI() {
                return new OpenAPI().info(new Info().title("EShop API").description("API documentation (fallback mode)").version("unknown"));
        }

}