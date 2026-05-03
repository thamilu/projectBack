package com.eshop.app.config.security;

import com.eshop.app.config.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * Keycloak OAuth2 Security Configuration - Dual Realm Support
 *
 * <h2>Features:</h2>
 * <ul>
 * <li><b>/api/admin/**</b> -> Validated against <b>eshop-admin</b> realm</li>
 * <li><b>/api/**</b> -> Validated against <b>eshop</b> (user) realm</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@org.springframework.context.annotation.Profile("keycloak")
@RequiredArgsConstructor
@Slf4j
public class KeycloakSecurityConfig {

    private final AppProperties appProperties;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String userRealmIssuer;

    @Value("${app.security.admin.issuer-uri}")
    private String adminRealmIssuer;

    /**
     * 🔐 Chain 1: ADMIN API -> Validates with 'eshop-admin' realm
     * Higher priority (@Order(1)) to intercept /api/admin/**
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("🛡️ Configuring ADMIN Security Chain for /api/admin/** (Issuer: {})", adminRealmIssuer);

        http
                .securityMatcher(
                        "/api/admin/**",
                        "/api/v1/admin/**",
                        "/api/v1/sellers/requests/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Explicitly require ADMIN role for confirmation, though realm check is the
                        // primary gate
                        .anyRequest().hasRole("ADMIN"))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(adminJwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    /**
     * 🛒 Chain 2: USER API -> Validates with 'eshop' realm
     * Handles everything else
     */
    @Bean
    @Order(2)
    public SecurityFilterChain userSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("🛡️ Configuring USER Security Chain for /api/** (Issuer: {})", userRealmIssuer);

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                                "/swagger-ui/**", "/v3/api-docs/**",
                                "/actuator/health", "/actuator/info",
                                "/api/public/**",
                                "/api/auth/**"
                ).permitAll()

                        // Public read-only product/category endpoints
                .requestMatchers(org.springframework.http.HttpMethod.GET, 
                    "/api/v1/products/**",
                    "/api/v1/categories/**",
                    "/api/v1/brands/**",
                    "/api/v1/shops/**",
                    "/api/v1/stores/**"
                ).permitAll()

                        // Seller onboarding and public checks (Authenticated but not yet SELLER)
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/sellers/register")
                        .authenticated()
                        .requestMatchers("/api/v1/sellers/profile/exists", "/api/v1/sellers/profile").authenticated()

                        // Seller-only endpoints
                        .requestMatchers("/api/seller/**", "/api/v1/seller/**", "/api/v1/sellers/**",
                                "/api/v1/dashboard/seller/**")
                    .hasRole("SELLER")

                // Delivery endpoints
                .requestMatchers("/api/delivery/**", "/api/v1/dashboard/delivery-agent/**")
                    .hasRole("DELIVERY_AGENT")

                        // Default: Authenticated (Customer or any valid user)
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                                .decoder(userJwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    /**
     * Decoder for ADMIN Realm
     */
    @Bean
    public JwtDecoder adminJwtDecoder() {
        return NimbusJwtDecoder.withIssuerLocation(adminRealmIssuer).build();
    }

    /**
     * Decoder for USER Realm
     */
    @Bean
    public JwtDecoder userJwtDecoder() {
        return NimbusJwtDecoder.withIssuerLocation(userRealmIssuer).build();
    }

    /**
     * Role Converter (shared for both, logic is same: realm_access.roles)
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            java.util.List<org.springframework.security.core.GrantedAuthority> authorities = new java.util.ArrayList<>();
            // Map realm roles
            java.util.Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null) {
                @SuppressWarnings("unchecked")
                java.util.List<String> roles = (java.util.List<String>) realmAccess.get("roles");
                if (roles != null) {
                    roles.forEach(role -> authorities.add(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                    "ROLE_" + role.toUpperCase())));
                }
            }
            // Also map client/resource roles if present (Keycloak may populate
            // resource_access)
            java.util.Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null && resourceAccess instanceof java.util.Map) {
                for (Object innerObj : ((java.util.Map<?, ?>) resourceAccess).values()) {
                    if (innerObj instanceof java.util.Map) {
                        Object r = ((java.util.Map<?, ?>) innerObj).get("roles");
                        if (r instanceof java.util.List) {
                            for (Object ro : (java.util.List<?>) r) {
                                if (ro != null) {
                                    authorities
                                            .add(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                    "ROLE_" + ro.toString().toUpperCase()));
                                }
                            }
                        }
                    }
                }
            }
            return authorities;
        });
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        AppProperties.Cors cors = appProperties.getCors();

        if (cors.getAllowedOrigins() != null && !cors.getAllowedOrigins().isBlank()) {
            config.setAllowedOriginPatterns(Arrays.asList(cors.getAllowedOrigins().split(",")));
        } else {
            log.error("❌ CORS allowed origins not configured! Blocking all requests.");
            config.setAllowedOrigins(Collections.emptyList());
        }

        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(
                Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "X-Request-ID", "Accept", "Origin"));
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Request-ID"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
