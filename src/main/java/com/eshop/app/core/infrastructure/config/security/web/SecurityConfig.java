package com.eshop.app.core.infrastructure.config.security.web;

import com.eshop.app.core.infrastructure.config.security.oauth.PrincipalDetails;

import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import com.eshop.app.user.application.security.JwtClaimExtractor;
import com.eshop.app.user.application.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.*;

/**
 * Hardened Consolidated Security Configuration
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
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;
    private final JwtClaimExtractor jwtClaimExtractor;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String userRealmIssuer;

    @Value("${app.security.admin.issuer-uri:}")
    private String adminRealmIssuer;

    // Caches the (keycloakId -> local user ID) sync outcome for a short window so the
    // identity-sync DB round trip does not run on every single authenticated request —
    // this is a stateless resource server, so without this, every request re-derives the
    // Authentication from the raw JWT, and syncUserIdentity's DB writes previously ran
    // unconditionally each time. Safe from a real-time-authorization standpoint: role
    // enforcement (@PreAuthorize/hasRole) reads authorities built directly from the JWT's
    // own claims every request (see extractAuthorities/extractRawRoles below), which is
    // NOT affected by this cache — only the background bookkeeping write to the local
    // `users` table (profile fields, locally-synced role) is deferred, for up to the TTL.
    private final Cache<String, Long> identitySyncCache = Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfterWrite(Duration.ofSeconds(60))
            .build();

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
     * Chain 1: ADMIN API -> Validates with the admin realm. Only registered when
     * {@code app.security.admin.issuer-uri} is configured — {@code @ConditionalOnProperty}
     * is the idiomatic way to express "this bean only exists when this property is set,"
     * clearer than the property being present-but-blank (its {@code @Value} default is
     * {@code ""}, never {@code null}) and returning {@code null} from the factory method.
     */
    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "app.security.admin.issuer-uri")
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("[SECURITY] Configuring ADMIN security chain (issuer: {})", adminRealmIssuer);

        http
                .securityMatcher(
                        "/api/admin/**",
                        "/api/v1/admin/**",
                        "/api/v1/sellers/requests",
                        "/api/v1/sellers/requests/**",
                        "/api/v1/dashboard/admin",
                        "/api/v1/dashboard/admin/**"
                )
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Defense in depth beyond the ADMIN role check below: the shared,
                        // multi-tenant JwtDecoder bean correctly verifies signatures for
                        // EITHER realm (issuer-based routing — see MultiTenantJwtConfiguration),
                        // so a role-claim misconfiguration on the user realm's side is the
                        // only thing standing between a non-admin-realm token and this chain
                        // without this check. Pinning the issuer here means admin endpoints
                        // reject any token not actually issued by the admin realm outright,
                        // regardless of what role claims it carries.
                        .anyRequest().access(adminRealmIssuerMatches())
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(new CustomBearerTokenResolver(objectMapper))
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoderProvider.getIfAvailable(
                                        () -> NimbusJwtDecoder.withIssuerLocation(adminRealmIssuer).build()))
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    /**
     * Requires BOTH the admin role AND that the authenticated token's issuer is the
     * admin realm. Role check first (cheap, and produces the existing 403 semantics for
     * an admin-realm-but-non-admin-role token); issuer check second, as the
     * defense-in-depth layer described above.
     */
    private org.springframework.security.authorization.AuthorizationManager<RequestAuthorizationContext> adminRealmIssuerMatches() {
        String requiredRole = "ROLE_" + appProperties.getSecurity().getRoles().getAdmin();
        return (authentication, context) -> {
            Authentication auth = authentication.get();
            boolean hasAdminRole = auth != null && auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(requiredRole::equals);
            boolean isAdminRealmToken = auth != null && auth.getCredentials() instanceof Jwt jwt
                    && adminRealmIssuer.equals(jwt.getIssuer() != null ? jwt.getIssuer().toString() : null);
            return new AuthorizationDecision(hasAdminRole && isAdminRealmToken);
        };
    }

    /**
     * Chain 2: USER API -> Primary application endpoints
     */
    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("[SECURITY] Configuring PRIMARY security chain (issuer: {})", userRealmIssuer);
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
                        // Payment gateway webhooks: gateways cannot present a Keycloak bearer
                        // token, so these must be public — authenticity is instead enforced by
                        // per-gateway signature verification inside ProcessPaymentUseCaseImpl.
                        .requestMatchers(HttpMethod.POST, "/api/v1/webhooks/**")
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
                        .requestMatchers("/api/v1/sellers/verify/**").authenticated()

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
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    // --- JWT Conversion & Identity Syncing ---

    @Bean
    public org.springframework.core.convert.converter.Converter<Jwt, org.springframework.security.authentication.AbstractAuthenticationToken> jwtAuthenticationConverter() {
        log.debug("[SECURITY] Initializing JWT converter with identity sync");

        return jwt -> {
            // Read at invocation time, not bean-construction time, in case
            // app.security.role-prefix is ever changed via a config refresh mechanism.
            String rolePrefix = appProperties.getSecurity().getRolePrefix();

            // 1. Extract Authorities (Roles) — read fresh from the JWT's own claims on
            // every request; NOT affected by identitySyncCache below.
            Collection<GrantedAuthority> authorities = extractAuthorities(jwt, rolePrefix);
            Set<String> rawRoles = extractRawRoles(jwt);

            // 2. Resolve/Sync Local Identity
            String keycloakId = jwt.getSubject();
            Long localUserId = syncUserIdentity(keycloakId, jwt, rawRoles);

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

    private Long syncUserIdentity(String keycloakId, Jwt jwt, Set<String> roles) {
        // Caffeine's cache throws NPE on a null key (ConcurrentHashMap.get rejects it) rather
        // than treating it as "not present" — a JWT with no 'sub' claim (malformed token,
        // non-compliant IdP response, or upstream misconfiguration) previously crashed the
        // entire request with a 500 instead of degrading gracefully. 'sub' is normally
        // mandatory for an OIDC access token, so this should not happen in a healthy
        // deployment, but a single anomalous token must not take down the whole endpoint.
        if (keycloakId == null || keycloakId.isBlank()) {
            log.error(
                    "[SECURITY][CRITICAL] JWT has no 'sub' claim — cannot resolve local user identity. "
                            + "Proceeding without identity sync (principal id will be -1).");
            return null;
        }

        Long cached = identitySyncCache.getIfPresent(keycloakId);
        if (cached != null) {
            return cached;
        }
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
                identitySyncCache.put(keycloakId, id);
                return id;
            }
        } catch (Exception e) {
            log.error(
                    "[SECURITY][CRITICAL] Identity sync failed for Keycloak user {} (email: {}): {}. This will cause 500 errors in downstream services.",
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

    /**
     * Delegates to {@link JwtClaimExtractor#extractEffectiveRoles(Jwt)} — the single,
     * canonical implementation of Keycloak role-claim navigation (realm + client-scoped
     * + root-level + groups), shared with diagnostic endpoints ({@code SessionController},
     * {@code MeController}) so the roles they report a user always match the
     * {@code GrantedAuthority} set actually driving {@code @PreAuthorize} decisions here.
     */
    private Set<String> extractRawRoles(Jwt jwt) {
        return new LinkedHashSet<>(jwtClaimExtractor.extractEffectiveRoles(jwt));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        AppProperties.Cors cors = appProperties.getCors();

        List<String> allowedOrigins = Arrays.stream(cors.getAllowedOrigins().split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        if (allowedOrigins.contains("*") && cors.isAllowCredentials()) {
            // Spring's CorsConfiguration itself rejects this combination at request time,
            // but failing fast here at startup gives a clear, actionable error instead of
            // a runtime CORS failure discovered later.
            throw new IllegalStateException(
                    "Invalid CORS configuration: allowed-origins contains a wildcard ('*') while " +
                    "allow-credentials is true. A wildcard origin combined with credentials would " +
                    "permit any origin to make credentialed requests. Configure explicit origins.");
        }

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "X-Request-ID",
                "X-Idempotency-Key", "X-Correlation-ID", "Accept", "Origin"));
        // Authorization intentionally NOT exposed: ExposedHeaders controls which response
        // headers cross-origin JavaScript may read. There is no legitimate reason for
        // client-side JS to read an Authorization *response* header, and exposing it
        // would matter the moment anything ever echoes it back.
        config.setExposedHeaders(Arrays.asList("Content-Type", "X-Request-ID", "X-Idempotency-Key", "X-Correlation-ID"));
        config.setAllowCredentials(cors.isAllowCredentials());
        config.setMaxAge(cors.getMaxAge());
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Custom Bearer Token Resolver that treats a bearer token as absent (rather than
     * forwarding it to the JWT decoder) for requests matching this app's public URL
     * whitelist.
     *
     * <p>This exists because of a genuine Spring Security OAuth2 resource-server
     * behavior, not a misunderstanding of {@code permitAll()}: the resource-server
     * filter authenticates a PRESENTED bearer token before the authorization decision is
     * made, regardless of whether the target endpoint actually requires authentication.
     * A guest browser with a stale/expired token still attached (e.g. leftover in
     * localStorage) would otherwise get 401'd on public pages like product listings,
     * purely because it presented an invalid credential — even though the endpoint
     * itself permits anonymous access. Dropping the token before Spring's filter sees it
     * makes the request look like a plain anonymous request instead.</p>
     */
    @Slf4j
    private static class CustomBearerTokenResolver implements org.springframework.security.oauth2.server.resource.web.BearerTokenResolver {
        private final org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver defaultResolver;

        public CustomBearerTokenResolver(ObjectMapper objectMapper) {
            this.defaultResolver = new org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver();
            // Bearer tokens must only be presented via the Authorization header — never a
            // query parameter (ends up in access logs, browser history, Referer headers
            // sent to third parties) or a form-encoded body parameter.
            this.defaultResolver.setAllowUriQueryParameter(false);
            this.defaultResolver.setAllowFormEncodedBodyParameter(false);
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
            // For every other endpoint, delegate entirely to the standard resolver and
            // let the real JwtDecoder's own validators (signature, expiry, issuer,
            // audience — see MultiTenantJwtConfiguration) make the call. A prior version
            // of this method additionally pre-parsed the token's payload here to check
            // 'exp' before signature verification, to short-circuit expired tokens to
            // "anonymous" instead of letting the decoder reject them with 401. That
            // pre-check was removed: it produced the exact same end result (401 on a
            // protected endpoint either way) while relying on unverified, attacker-
            // modifiable payload data and duplicating validation the decoder already
            // performs correctly, post-signature-verification.
            return defaultResolver.resolve(request);
        }
    }
}
