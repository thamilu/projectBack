package com.eshop.app.config;

import com.eshop.app.config.properties.AppProperties;
import com.eshop.app.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import com.eshop.app.security.PrincipalDetails;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
@Slf4j
public class EnhancedSecurityConfig {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final org.springframework.beans.factory.ObjectProvider<com.eshop.app.service.UserService> userServiceProvider;

    // Using com.eshop.app.security.PrincipalDetails
    
    @Bean
    @Order(0)
    public SecurityFilterChain staticResourcesChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(
                "/favicon.ico",
                "/static/**",
                "/public/**",
                "/error",
                "/css/**",
                "/js/**",
                "/images/**"
            )
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .securityContext(context -> context.disable())
            .sessionManagement(session -> session.disable())
            .requestCache(cache -> cache.disable());
        return http.build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/actuator/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                .requestMatchers("/actuator/prometheus").hasRole(appProperties.getSecurity().getRoles().getAdmin())
                .anyRequest().hasRole(appProperties.getSecurity().getRoles().getAdmin())
            )
            .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http, org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder) throws Exception {
        log.info("Configuring security filter chain with OAuth2 Resource Server");
        AppProperties.Security.Roles roles = appProperties.getSecurity().getRoles();
        
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/csp/report").permitAll()
                .requestMatchers("/auth/session").permitAll()
                .requestMatchers("/auth/config").permitAll()
                        .requestMatchers("/api/v1/public/**", "/api/auth/**", "/auth/_log", "/api/debug/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", "/error").permitAll()
                
                        .requestMatchers("/actuator/**").hasRole(roles.getAdmin())

                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/**")
                        .hasAnyRole(roles.getSeller(), roles.getAdmin())
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**")
                        .hasAnyRole(roles.getSeller(), roles.getAdmin())
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole(roles.getAdmin())

                .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                        .requestMatchers("/api/v1/categories/**").hasRole(roles.getAdmin())
                
                        .requestMatchers("/api/v1/admin/**").hasRole(roles.getAdmin())
                
                        .requestMatchers("/api/v1/sellers/{id}/sync-role").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/sellers/register").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/sellers/identity-types", "/api/v1/sellers/business-types").authenticated()
                        .requestMatchers("/api/v1/sellers/profile/exists", "/api/v1/sellers/profile").authenticated()
                        .requestMatchers("/api/v1/sellers/**").hasAnyRole(roles.getSeller(), roles.getAdmin())
                        
                        .requestMatchers("/api/v1/cart/**").hasAnyRole(roles.getCustomer(), roles.getAdmin())

                        .requestMatchers("/api/v1/orders/**").authenticated()
                .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()).decoder(jwtDecoder))
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            )
                .headers(headers -> {
                    AppProperties.Security.Headers secHeaders = appProperties.getSecurity().getHeaders();
                    if (secHeaders.isEnabled()) {
                        headers.contentSecurityPolicy(
                                csp -> csp.policyDirectives(secHeaders.getContentSecurityPolicy()))
                                .frameOptions(frame -> {
                                    if ("DENY".equalsIgnoreCase(secHeaders.getXFrameOptions()))
                                        frame.deny();
                                    else if ("SAMEORIGIN".equalsIgnoreCase(secHeaders.getXFrameOptions()))
                                        frame.sameOrigin();
                                })
                                .httpStrictTransportSecurity(
                                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000));
                    }
                });

        return http.build();
    }

    private java.util.Set<String> extractRoles(org.springframework.security.oauth2.jwt.Jwt jwt) {
        java.util.Set<String> allRoles = new java.util.HashSet<>();
        AppProperties.Security sec = appProperties.getSecurity();
        String claimRealms = sec.getClaimRealms();
        String claimRoles = sec.getClaimRoles();

        try {
            java.util.List<String> topLevelRoles = jwt.getClaim(claimRoles);
            if (topLevelRoles != null)
                allRoles.addAll(topLevelRoles);
        } catch (Exception ignored) {
        }

        java.util.Map<String, Object> realmAccess = jwt.getClaim(claimRealms);
        if (realmAccess != null) {
            @SuppressWarnings("unchecked")
            java.util.List<String> realmRoles = (java.util.List<String>) realmAccess.get(claimRoles);
            if (realmRoles != null)
                allRoles.addAll(realmRoles);
        }

        java.util.Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            resourceAccess.values().forEach(resource -> {
                if (resource instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> resourceMap = (java.util.Map<String, Object>) resource;
                    @SuppressWarnings("unchecked")
                    java.util.List<String> resourceRoles = (java.util.List<String>) resourceMap.get(claimRoles);
                    if (resourceRoles != null)
                        allRoles.addAll(resourceRoles);
                }
            });
        }
        return allRoles;
    }

    @Bean
    public org.springframework.core.convert.converter.Converter<org.springframework.security.oauth2.jwt.Jwt, org.springframework.security.authentication.AbstractAuthenticationToken> jwtAuthenticationConverter() {
        log.info("🔧 EnhancedSecurityConfig JWT converter - extracting roles and mapping principal");
        AppProperties.Security sec = appProperties.getSecurity();
        String rolePrefix = sec.getRolePrefix();

        JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
        delegate.setJwtGrantedAuthoritiesConverter(jwt -> {
            java.util.List<org.springframework.security.core.GrantedAuthority> authorities = new java.util.ArrayList<>();
            java.util.Set<String> roles = extractRoles(jwt);

            roles.forEach(role -> {
                if (role != null && !role.isBlank() && !role.startsWith("default-")) {
                    authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                            rolePrefix + role.toUpperCase()));
                }
            });

            return authorities;
        });

        return jwt -> {
            org.springframework.security.core.Authentication auth = delegate.convert(jwt);
            java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities = auth != null
                    ? auth.getAuthorities()
                    : List.of();

            java.util.Set<String> allRoles = extractRoles(jwt);

            String username = jwt.getClaimAsString("preferred_username");
            String email = jwt.getClaimAsString("email");
            String givenName = jwt.getClaimAsString("given_name");
            String familyName = jwt.getClaimAsString("family_name");
            String phoneNumber = jwt.getClaimAsString("phone_number");
            Boolean emailVerified = jwt.getClaim("email_verified");
            String keycloakId = jwt.getSubject();
            if (keycloakId == null)
                keycloakId = jwt.getClaimAsString("user_id");
            if (keycloakId == null)
                keycloakId = jwt.getClaimAsString("sub");

            Long localUserId = null;
            try {
                com.eshop.app.service.UserService userService = userServiceProvider.getIfAvailable();
                if (userService != null) {
                    java.util.Optional<Long> foundId = userService.findUserIdByKeycloakId(keycloakId);

                    if (foundId.isPresent()) {
                        localUserId = foundId.get();
                        userService.syncUserFromKeycloak(keycloakId, username, email, givenName, familyName, phoneNumber, emailVerified);
                        userService.syncUserRoles(localUserId, allRoles);
                    } else {
                        log.info("Creating new local user for Keycloak ID: {}", keycloakId);
                        localUserId = userService.syncUserFromKeycloak(keycloakId, username, email, givenName, familyName, phoneNumber, emailVerified);
                        userService.syncUserRoles(localUserId, allRoles);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to resolve/create local user for JWT subject {}", keycloakId, e);
            }

            PrincipalDetails principal = PrincipalDetails.builder()
                    .id(localUserId != null ? localUserId : -1L)
                    .username(username)
                    .email(email)
                    .keycloakId(keycloakId)
                    .build();
            return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, jwt, authorities);
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        AppProperties.Cors cors = appProperties.getCors();
        log.info("Configuring CORS with allowed origins: {}", cors.getAllowedOrigins());
        
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(cors.getAllowedOrigins().split(",")));
        configuration.setAllowedMethods(Arrays.asList(cors.getAllowedMethods().split(",")));
        configuration.setAllowedHeaders(Arrays.asList(cors.getAllowedHeaders().split(",")));
        configuration.setExposedHeaders(Arrays.asList(cors.getExposedHeaders().split(",")));
        configuration.setAllowCredentials(cors.isAllowCredentials());
        configuration.setMaxAge(cors.getMaxAge());
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message("Full authentication is required to access this resource")
                .path(request.getRequestURI())
                    .build();
            objectMapper.writeValue(response.getOutputStream(), errorResponse);
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message("You don't have permission to access this resource")
                .path(request.getRequestURI())
                    .build();
            objectMapper.writeValue(response.getOutputStream(), errorResponse);
        };
    }
}
