package com.eshop.app.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.eshop.app.constants.ApiConstants;

@Configuration
public class SwaggerGroupConfig {

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("public")
                .displayName("Public API")
            .pathsToMatch(ApiConstants.BASE_PATH + "/public/**", ApiConstants.BASE_PATH + "/products/**", ApiConstants.BASE_PATH + "/categories/**")
            .build();
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
            .group("auth")
                .displayName("Authentication / Keycloak API")
            .pathsToMatch(ApiConstants.BASE_PATH + "/auth/**")
            .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin-api")
                .displayName("Admin API")
            .pathsToMatch(ApiConstants.BASE_PATH + "/admin/**")
            .build();
    }

    @Bean
    public GroupedOpenApi sellerApi() {
        return GroupedOpenApi.builder()
            .group("seller")
                .displayName("Seller API")
            .pathsToMatch(ApiConstants.BASE_PATH + "/seller/**")
            .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
            .group("user")
                .displayName("User API")
            .pathsToMatch(ApiConstants.BASE_PATH + "/users/**", ApiConstants.BASE_PATH + "/orders/**", ApiConstants.BASE_PATH + "/cart/**")
            .build();
    }

    @Bean
    public GroupedOpenApi allApis() {
        return GroupedOpenApi.builder()
            .group("all")
                .displayName("All API")
            .pathsToMatch(ApiConstants.API_PREFIX + "/**")
            .build();
    }

    @Bean
    public GroupedOpenApi allPackages() {
        return GroupedOpenApi.builder()
            .group("all-packages")
            .displayName("All Packages")
            .packagesToScan("com.eshop.app")
            .build();
    }
}
