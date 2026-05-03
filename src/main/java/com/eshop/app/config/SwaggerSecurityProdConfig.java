package com.eshop.app.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.context.annotation.Bean;

/**
 * Restrict access to Swagger/OpenAPI endpoints in production.
 */
@Configuration
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class SwaggerSecurityProdConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Registering production Swagger security: requiring ROLE_ADMIN for docs");

        http
                .securityMatcher("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html")
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ADMIN"))
                .csrf(csrf -> csrf.disable())
                .requestCache(rc -> rc.disable())
        ;

        return http.build();
    }
}
