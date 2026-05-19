package com.eshop.app.core.infrastructure.config.security.web;

import com.eshop.app.core.infrastructure.config.security.oauth.PrincipalDetails;

import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import com.eshop.app.core.api.response.ApiError;
import com.eshop.app.user.application.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.*;

/**
 * ðŸ›¡ï¸ Hardened Consolidated Security Configuration
 * 
 * Unified security policy for the E-Shop platform.
 * Supports dual-realm validation (Admin/User) and automatic user identity
 * syncing.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<UserService> userServiceProvider;
    private final ObjectProvider<JwtDecoder> jwtDecoderProvider;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String userRealmIssuer;

    @Value("${app.security.admin.issuer-uri:}")
    private String adminRealmIssuer;

    // --- Security Filter Chains ---

    @Bean
    @Order(0)
    public SecurityFilterChain staticResourcesChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/favicon.ico", "/static/**", "/public/**", "/error", "/css/**", "/js/**",
                        "/images/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .securityContext(AbstractHttpConfigurer::disable)
                .sessionManagement(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /**
     * ðŸ” Chain 1: ADMIN API -> Validates with 'eshop-admin' realm if configured
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        if (adminRealmIssuer == null || adminRealmIssuer.isBlank()) {
            log.warn("âš ï¸ Admin Realm Issuer not configured. Admin chain will be inactive.");
            return null;
        }

        log.info("ðŸ›¡ï¸ Configuring ADMIN Security Chain (Issuer: {})", adminRealmIssuer);

        http
                .securityMatcher("/api/admin/**", "/api/v1/admin/**", "/api/v1/sellers/requests/**",
                        "/api/v1/dashboard/admin/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth -> auth.anyRequest().hasRole(appProperties.getSecurity().getRoles().getAdmin()))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(new CustomBearerTokenResolver(objectMapper))
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoderProvider.getIfAvailable(
                                        () -> NimbusJwtDecoder.withIssuerLocation(adminRealmIssuer).build()))
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()));

        return http.build();
    }

    /**
     * ðŸ›’ Chain 2: USER API -> Primary application endpoints
     */
    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("ðŸ›¡ï¸ Configuring PRIMARY Security Chain (Issuer: {})", userRealmIssuer);
        AppProperties.Security.Roles roles = appProperties.getSecurity().getRoles();

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/v1/public/**", "/v1/public/**",
                                "/api/v1/auth/**", "/v1/auth/**",
                                "/api/v1/csp/**", "/v1/csp/**",
                                "/api/v1/locations/**", "/v1/locations/**",
                                "/swagger-ui/**", "/v3/api-docs/**", "/error")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/products/**", "/v1/products/**",
                                "/api/v1/categories/**", "/v1/categories/**",
                                "/api/v1/brands/**", "/v1/brands/**",
                                "/api/v1/stores/**", "/v1/stores/**")
                        .permitAll()

                        // Seller onboarding (Authenticated but not yet SELLER role)
                        .requestMatchers(HttpMethod.POST, "/api/v1/sellers/register").authenticated()
                        .requestMatchers("/api/v1/sellers/profile/exists", "/api/v1/sellers/profile").authenticated()
                        .requestMatchers("/api/v1/sellers/check-handle/**").authenticated()

                        // Role-based restrictions
                        .requestMatchers("/api/v1/sellers/**").hasAnyRole(roles.getSeller(), roles.getAdmin())
                        .requestMatchers("/api/v1/cart/**").authenticated()
                        .requestMatchers("/api/v1/dashboard/customer").authenticated()
                        .requestMatchers("/api/v1/dashboard/seller").hasAnyRole(roles.getSeller(), roles.getAdmin())
                        .requestMatchers("/api/v1/orders/**").authenticated()
                        .requestMatchers("/actuator/**").hasRole(roles.getAdmin())

                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(new CustomBearerTokenResolver(objectMapper))
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoderProvider.getIfAvailable(
                                        () -> NimbusJwtDecoder.withIssuerLocation(userRealmIssuer).build()))
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()));

        return http.build();
    }

    // --- JWT Conversion & Identity Syncing ---

    @Bean
    public org.springframework.core.convert.converter.Converter<Jwt, org.springframework.security.authentication.AbstractAuthenticationToken> jwtAuthenticationConverter() {
        log.debug("ðŸ”§ Initializing JWT Converter with Identity Sync");
        AppProperties.Security sec = appProperties.getSecurity();
        String rolePrefix = sec.getRolePrefix();

        return jwt -> {
            // 1. Extract Authorities (Roles)
            Collection<GrantedAuthority> authorities = extractAuthorities(jwt, rolePrefix);
            Set<String> rawRoles = extractRawRoles(jwt);

            // 2. Resolve/Sync Local Identity
            String keycloakId = jwt.getSubject();
            Long localUserId = syncUserIdentity(jwt, rawRoles);

            // 3. Construct Principal
            String emailClaim = jwt.getClaimAsString("email");
            PrincipalDetails principal = PrincipalDetails.builder()
                    .id(localUserId != null ? localUserId : -1L)
                    .email(emailClaim != null ? emailClaim : keycloakId)
                    .keycloakId(keycloakId)
                    .build();

            return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
        };
    }

    private Long syncUserIdentity(Jwt jwt, Set<String> roles) {
        String keycloakId = jwt.getSubject();
        try {
            UserService userService = userServiceProvider.getIfAvailable();
            if (userService != null) {
                String email = jwt.getClaimAsString("email");
                String firstName = jwt.getClaimAsString("given_name");
                String lastName = jwt.getClaimAsString("family_name");
                String phone = jwt.getClaimAsString("phone_number");
                Boolean emailVerified = jwt.getClaim("email_verified");

                Long id = userService.syncUserFromKeycloak(keycloakId, email, firstName, lastName, phone,
                        emailVerified);
                userService.syncUserRoles(id, roles);
                return id;
            }
        } catch (Exception e) {
            log.error(
                    "ðŸš¨ CRITICAL: Identity sync failed for Keycloak user {} (email: {}): {}. This will cause 500 errors in downstream services.",
                    keycloakId, jwt.getClaimAsString("email"), e.getMessage(), e);
        }
        return null;
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt, String prefix) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        extractRawRoles(jwt).forEach(role -> {
            if (role != null && !role.isBlank() && !role.startsWith("default-")) {
                authorities.add(new SimpleGrantedAuthority(prefix + role.toUpperCase()));
            }
        });
        return authorities;
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractRawRoles(Jwt jwt) {
        Set<String> roles = new HashSet<>();
        AppProperties.Security sec = appProperties.getSecurity();

        log.debug("DEBUG: Extracting roles from JWT. Subject: {}, Issuer: {}", jwt.getSubject(), jwt.getIssuer());

        // 1. Realm Roles (Standard Keycloak)
        Object rolesObj = null;
        if (sec.getClaimRealms().contains(".")) {
            // Handle composite path like realm_access.roles
            String[] parts = sec.getClaimRealms().split("\\.");
            Map<String, Object> current = jwt.getClaims();
            for (int i = 0; i < parts.length - 1; i++) {
                Object next = current.get(parts[i]);
                if (next instanceof Map) {
                    current = (Map<String, Object>) next;
                } else {
                    current = null;
                    break;
                }
            }
            if (current != null) {
                rolesObj = current.get(parts[parts.length - 1]);
            }
        } else {
            Map<String, Object> realmAccess = jwt.getClaim(sec.getClaimRealms());
            if (realmAccess != null) {
                rolesObj = realmAccess.get(sec.getClaimRoles());
            }
        }

        if (rolesObj instanceof List) {
            List<String> realmRoles = (List<String>) rolesObj;
            roles.addAll(realmRoles);
            log.debug("DEBUG: Found realm roles via path {}: {}", sec.getClaimRealms(), realmRoles);
        }

        // 2. Resource/Client Roles (Keycloak Client Roles)
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            resourceAccess.values().forEach(resource -> {
                if (resource instanceof Map && ((Map<?, ?>) resource).get(sec.getClaimRoles()) instanceof List) {
                    List<String> clientRoles = (List<String>) ((Map<?, ?>) resource).get(sec.getClaimRoles());
                    roles.addAll(clientRoles);
                    log.debug("DEBUG: Found client roles: {}", clientRoles);
                }
            });
        }

        // 3. Root-level 'roles' claim (Alternative/Simplified structure)
        if (jwt.hasClaim("roles") && jwt.getClaim("roles") instanceof List) {
            List<String> rootRoles = jwt.getClaim("roles");
            roles.addAll(rootRoles);
            log.debug("DEBUG: Found root-level roles: {}", rootRoles);
        }

        // 4. 'groups' claim (Commonly used in OIDC for organizational roles)
        if (jwt.hasClaim("groups") && jwt.getClaim("groups") instanceof List) {
            List<String> groups = jwt.getClaim("groups");
            roles.addAll(groups);
            log.debug("DEBUG: Found roles in 'groups' claim: {}", groups);
        }

        log.info("DEBUG: Final extracted roles for user {}: {}", jwt.getSubject(), roles);
        return roles;
    }

    // --- Error Handlers ---

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, ex) -> {
            log.error("âŒ Authentication error on {}: {}", request.getRequestURI(), ex.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiError error = ApiError.of(401, "Unauthorized", "Authentication required", request.getRequestURI());
            objectMapper.writeValue(response.getOutputStream(), error);
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> {
            // [HARDEN] Log principal + authorities to diagnose exactly why 403 was issued
            String principal = "anonymous";
            try {
                org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated()) {
                    principal = auth.getName() + " authorities=" + auth.getAuthorities();
                }
            } catch (Exception ignored) {
            }
            log.warn("ðŸš« [ACCESS_DENIED] {} {} | principal={} | reason={}",
                    request.getMethod(), request.getRequestURI(), principal, ex.getMessage());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiError error = ApiError.of(403, "Forbidden", "You do not have permission to perform this action",
                    request.getRequestURI());
            objectMapper.writeValue(response.getOutputStream(), error);
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        AppProperties.Cors cors = appProperties.getCors();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.asList(cors.getAllowedOrigins().split(",")));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "X-Request-ID",
                "X-Idempotency-Key", "Accept", "Origin"));
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Request-ID", "X-Idempotency-Key"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * [HARDEN] Custom Bearer Token Resolver that bypasses expired JWT validation
     * to avoid 401 Unauthorized on whitelisted public endpoints for guests with stale sessions.
     */
    @Slf4j
    private static class CustomBearerTokenResolver implements org.springframework.security.oauth2.server.resource.web.BearerTokenResolver {
        private final org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver defaultResolver = 
                new org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver();
        private final ObjectMapper objectMapper;

        public CustomBearerTokenResolver(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        private boolean isPublicEndpoint(jakarta.servlet.http.HttpServletRequest request) {
            String uri = request.getRequestURI();
            String method = request.getMethod();

            // Normalize uri by removing context path if present
            String contextPath = request.getContextPath();
            if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
                uri = uri.substring(contextPath.length());
            }

            // Public patterns matching SecurityConfig whitelists
            if (uri.startsWith("/api/v1/public/") || uri.startsWith("/v1/public/") ||
                uri.startsWith("/api/v1/auth/") || uri.startsWith("/v1/auth/") ||
                uri.startsWith("/api/v1/csp/") || uri.startsWith("/v1/csp/") ||
                uri.startsWith("/api/v1/locations/") || uri.startsWith("/v1/locations/") ||
                uri.startsWith("/swagger-ui/") || uri.startsWith("/v3/api-docs/") ||
                "/error".equals(uri)) {
                return true;
            }

            if ("GET".equalsIgnoreCase(method)) {
                if (uri.startsWith("/api/v1/products/") || uri.startsWith("/v1/products/") ||
                    uri.startsWith("/api/v1/categories/") || uri.startsWith("/v1/categories/") ||
                    uri.startsWith("/api/v1/brands/") || uri.startsWith("/v1/brands/") ||
                    uri.startsWith("/api/v1/stores/") || uri.startsWith("/v1/stores/")) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public String resolve(jakarta.servlet.http.HttpServletRequest request) {
            if (isPublicEndpoint(request)) {
                log.debug("Public endpoint detected: {}. Bypassing bearer token resolution to allow anonymous guest access.", request.getRequestURI());
                return null;
            }

            String token = defaultResolver.resolve(request);
            if (token == null) {
                return null;
            }
            try {
                String[] parts = token.split("\\.");
                if (parts.length == 3) {
                    byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
                    Map<?, ?> payload = objectMapper.readValue(decoded, Map.class);
                    Number expNum = (Number) payload.get("exp");
                    if (expNum != null) {
                        long expTime = expNum.longValue();
                        long currentTime = System.currentTimeMillis() / 1000;
                        if (expTime < currentTime) {
                            log.info("Expired JWT detected (expired at {}). Treating as anonymous to avoid 401 on public endpoints.", expTime);
                            return null;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse JWT for expiration check: {}", e.getMessage());
                return null;
            }
            return token;
        }
    }
}

