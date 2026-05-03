package com.eshop.app.config;

import com.eshop.app.config.properties.AppProperties;
import com.eshop.app.config.properties.SwaggerProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

        private final AppProperties appProperties;
        private final SwaggerProperties swaggerProperties;

    @Bean
    public OpenAPI customOpenAPI() {
            log.info("OpenAPI documentation enabled with dynamic configuration");

            AppProperties.Openapi openapi = appProperties.getOpenapi();
            if (!openapi.isEnabled()) {
                    return new OpenAPI().info(new Info().title("Disabled"));
            }

        final String oauth2SchemeName = "oauth2";
        final String bearerJwtSchemeName = "bearer-jwt";

        final String authorizationUrl = appProperties.getSwagger().getSecurity().getAuthorizationUrl();
        final String tokenUrl = appProperties.getSwagger().getSecurity().getTokenUrl();

        String serverUrl = Optional.ofNullable(appProperties.getBackendUrl())
                        .filter(s -> !s.isBlank())
                        .orElseThrow(() -> new IllegalStateException(
                                        "Backend Server URL must be configured via APP_BACKEND_URL"));

        return new OpenAPI()
                .info(apiInfo())
                        .servers(List.of(new Server().url(serverUrl).description("API Server")))
                .addSecurityItem(new SecurityRequirement().addList(oauth2SchemeName))
                .addSecurityItem(new SecurityRequirement().addList(bearerJwtSchemeName))
                        .components(new Components()
                        .addSecuritySchemes(oauth2SchemeName,
                                new SecurityScheme()
                                        .name(oauth2SchemeName)
                                        .type(SecurityScheme.Type.OAUTH2)
                                                                        .description("OAuth2 Authentication")
                                        .flows(new OAuthFlows()
                                                .authorizationCode(new OAuthFlow()
                                                        .authorizationUrl(authorizationUrl)
                                                        .tokenUrl(tokenUrl)
                                                        .scopes(new Scopes()
                                                                .addString("openid", "OpenID Connect")
                                                                .addString("profile", "User profile")
                                                                                                                        .addString("email",
                                                                                                                                        "User email")))))
                        .addSecuritySchemes(bearerJwtSchemeName,
                                new SecurityScheme()
                                        .name(bearerJwtSchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token directly (format: 'Bearer <token>')")));
    }

    private Info apiInfo() {
            SwaggerProperties.ApiInfo apiInfo = swaggerProperties.getApiInfo();
            SwaggerProperties.Contact contact = apiInfo.getContact();
            SwaggerProperties.LicenseInfo license = apiInfo.getLicense();

        return new Info()
                        .title(apiInfo.getTitle())
                        .description(apiInfo.getDescription())
                        .version(apiInfo.getVersion())
                        .contact(new Contact()
                                        .name(contact.getName())
                                        .email(contact.getEmail())
                                        .url(contact.getUrl()))
                        .license(new License()
                                        .name(license.getName())
                                        .url(license.getUrl()));
    }
}
