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
 * Permissive Swagger security for non-production environments (allow access).
 */
@Configuration
@Profile("!prod")
@RequiredArgsConstructor
@Slf4j
public class SwaggerSecurityDevConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain swaggerPermissiveFilterChain(HttpSecurity http) throws Exception {
        log.info("Registering non-prod Swagger security: permitting all for docs");

        http
                .securityMatcher("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .requestCache(rc -> rc.disable())
        ;

        return http.build();
    }
}
