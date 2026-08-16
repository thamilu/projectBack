package com.eshop.app.user.api.controller;

import com.eshop.app.core.api.BaseController;
import com.eshop.app.core.api.response.ApiError;
import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import com.eshop.app.core.kernel.ApiConstants;
import com.eshop.app.core.util.CorrelationIdUtils;
import com.eshop.app.user.api.request.LoginRequest;
import com.eshop.app.user.api.request.RefreshTokenRequest;
import com.eshop.app.user.api.response.TokenResponse;
import com.eshop.app.user.api.response.UserInfoResponse;
import com.eshop.app.user.application.service.KeycloakAuthService;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Keycloak authentication endpoints (login, refresh, OAuth2 authorization-code flow,
 * token introspection, logout).
 *
 * <p><b>Known gap (tracked, not fixed here):</b> the OAuth2 {@code state} parameter issued by
 * {@link #getLoginUrl} is returned to the caller but not persisted server-side, so
 * {@link #handleCallback} cannot yet verify it — a CSRF exposure in the authorization code flow
 * (RFC 6749 §10.12). Closing this requires a short-TTL state store. This app's Redis
 * configuration is not reliably present in every environment (the {@code prod} profile
 * comments out its Redis connection properties and defaults {@code spring.cache.type} to
 * {@code caffeine}, a local cache), so hard-wiring a Redis-backed store into this
 * business-critical login path risked breaking authentication in whichever environment
 * doesn't actually have Redis available. This needs an explicit decision on the state-storage
 * mechanism before implementation, not a guess.
 */
@RestController
@RequestMapping(ApiConstants.Endpoints.AUTH)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Keycloak / authentication endpoints")
public class KeycloakAuthController extends BaseController {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String RATE_LIMITER_AUTH = "login";

    private final KeycloakAuthService authService;
    private final AppProperties appProperties;

    /*
     * Class-level @CrossOrigin("*") was removed: it overrode this app's centrally-managed,
     * environment-configured CorsConfigurationSource (see SecurityConfig#corsConfigurationSource,
     * driven by app.cors.allowed-origins) with a wildcard on this controller specifically — the
     * single most sensitive surface in the application (credential exchange, token issuance,
     * token introspection). Removing it lets this controller fall back to the same
     * properly-scoped CORS policy already protecting every other endpoint.
     */

    /**
     * PUBLIC - Login with username/email and password.
     *
     * <p>Frontend example:
     * <pre>
     * POST /api/auth/login
     * {
     *   "email": "john.doe@example.com",
     *   "password": "password123"
     * }
     * </pre>
     */
    @PostMapping("/login")
    @RateLimiter(name = RATE_LIMITER_AUTH)
    public Mono<ResponseEntity<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received");

        // Errors intentionally propagate (no onErrorResume) so AuthControllerExceptionHandler
        // can produce a structured ApiError with correlation ID/error code instead of an
        // empty-body status. KeycloakException (bad credentials) still yields 401; any other
        // unexpected failure (e.g. connectivity) correctly falls through to a 500 rather than
        // being misreported as "unauthorized".
        return authService.login(request)
                .map(ResponseEntity::ok);
    }

    /**
     * PUBLIC - Refresh access token.
     *
     * <p>Frontend example:
     * <pre>
     * POST /api/auth/refresh
     * { "refreshToken": "your-refresh-token" }
     * </pre>
     */
    @PostMapping("/refresh")
    @RateLimiter(name = RATE_LIMITER_AUTH)
    public Mono<ResponseEntity<TokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request received");

        // See login(): errors propagate to AuthControllerExceptionHandler instead of being
        // swallowed into an empty-body 401.
        return authService.refreshToken(request)
                .map(ResponseEntity::ok);
    }

    /**
     * PUBLIC - Get authorization URL for OAuth2 login.
     *
     * <p>Frontend example:
     * <pre>
     * GET /api/auth/login-url?redirectUri=https://yourapp.com/callback
     *
     * Response:
     * { "authorizationUrl": "https://keycloak.example.com/realms/.../auth?...", "state": "uuid" }
     * </pre>
     * Then redirect the user to {@code authorizationUrl} in the browser.
     *
     * @throws ResponseStatusException 400 if {@code redirectUri} is not a registered/allowed
     *                                  redirect target
     */
    @GetMapping("/login-url")
    public ResponseEntity<Map<String, String>> getLoginUrl(
            @RequestParam(required = false) String redirectUri) {

        String resolvedRedirectUri = resolveAndValidateRedirectUri(redirectUri);

        String state = UUID.randomUUID().toString();
        String authUrl = authService.getAuthorizationUrl(resolvedRedirectUri, state);

        log.info("Generated authorization URL with state: {}", state);

        Map<String, String> response = new HashMap<>();
        response.put("authorizationUrl", authUrl);
        response.put("state", state);
        response.put("message", "Redirect user to authorizationUrl");

        return ResponseEntity.ok(response);
    }

    /**
     * PUBLIC - OAuth2 callback (exchange authorization code for tokens).
     *
     * <p>Frontend example: after the user logs in via Keycloak, they're redirected to
     * {@code https://yourapp.com/callback?code=xxx&state=yyy}; the frontend then calls
     * {@code GET /api/auth/callback?code=xxx&redirectUri=https://yourapp.com/callback}.
     *
     * <p>{@code state} is accepted but not yet verified against the value issued by
     * {@link #getLoginUrl} — see this class's Javadoc.
     *
     * @throws ResponseStatusException 400 if {@code redirectUri} is not a registered/allowed
     *                                  redirect target
     */
    @GetMapping("/callback")
    public Mono<ResponseEntity<TokenResponse>> handleCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String redirectUri) {

        String resolvedRedirectUri = resolveAndValidateRedirectUri(redirectUri);

        log.info("OAuth2 callback received with code");

        // See login(): errors propagate to AuthControllerExceptionHandler instead of being
        // swallowed into an empty-body 400.
        return authService.exchangeAuthorizationCode(code, resolvedRedirectUri)
                .map(ResponseEntity::ok);
    }

    /**
     * Requires a Bearer token, which is forwarded to and validated by Keycloak's userinfo
     * endpoint (not by this app's local JWT decoder) — a malformed/expired/revoked token
     * surfaces as a {@code KeycloakException} from that call.
     *
     * <p>Frontend example:
     * <pre>
     * GET /api/auth/userinfo
     * Headers: Authorization: Bearer &lt;access-token&gt;
     * </pre>
     *
     * @throws ResponseStatusException 400 if the Authorization header is missing/malformed
     */
    @GetMapping("/userinfo")
    public Mono<ResponseEntity<UserInfoResponse>> getUserInfo(
            @RequestHeader("Authorization") String authHeader) {

        String token = extractBearerToken(authHeader);

        // See login(): errors propagate to AuthControllerExceptionHandler instead of being
        // swallowed into an empty-body 401.
        return authService.getUserInfo(token)
                .map(ResponseEntity::ok);
    }

    /**
     * Requires a valid Bearer token, validated by Spring Security's JWT resource-server filter
     * (this app's own local decoder, not a Keycloak round-trip); returns {@code 401} if the
     * principal is absent.
     *
     * <p>Frontend example:
     * <pre>
     * GET /api/auth/me
     * Headers: Authorization: Bearer &lt;access-token&gt;
     * </pre>
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt) {

        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("sub", jwt.getSubject());
        userInfo.put("email", jwt.getClaimAsString("email"));
        userInfo.put("name", jwt.getClaimAsString("name"));
        userInfo.put("givenName", jwt.getClaimAsString("given_name"));
        userInfo.put("familyName", jwt.getClaimAsString("family_name"));
        userInfo.put("emailVerified", jwt.getClaimAsBoolean("email_verified"));
        // extractRoles(jwt) (BaseController) correctly walks the nested realm_access.roles
        // structure; Jwt.getClaimAsStringList("realm_access.roles") does not — it only does a
        // literal top-level key lookup, so no claim is ever actually named "realm_access.roles"
        // and this field silently returned null for every caller.
        userInfo.put("roles", extractRoles(jwt));
        userInfo.put("issuedAt", jwt.getIssuedAt());
        userInfo.put("expiresAt", jwt.getExpiresAt());

        return ResponseEntity.ok(userInfo);
    }

    /**
     * Intentionally accepts any bearer-shaped token without prior local authentication — that's
     * the purpose of a token-introspection endpoint (RFC 7662): callers use it specifically to
     * ask "is this arbitrary token still valid," which Keycloak itself determines.
     *
     * <p>Frontend example:
     * <pre>
     * POST /api/auth/introspect
     * Headers: Authorization: Bearer &lt;access-token&gt;
     * </pre>
     *
     * @throws ResponseStatusException 400 if the Authorization header is missing/malformed
     */
    @PostMapping("/introspect")
    @RateLimiter(name = RATE_LIMITER_AUTH)
    public Mono<ResponseEntity<Map<String, Object>>> introspectToken(
            @RequestHeader("Authorization") String authHeader) {

        String token = extractBearerToken(authHeader);

        return authService.introspectToken(token)
                .map(result -> {
                    boolean active = result.get("active") instanceof Boolean value && value;
                    if (active) {
                        return ResponseEntity.ok(result);
                    } else {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
                    }
                });
    }

    /**
     * Logout user (invalidate refresh token).
     *
     * <p>Frontend example:
     * <pre>
     * POST /api/auth/logout
     * { "refreshToken": "your-refresh-token" }
     * </pre>
     */
    @PostMapping("/logout")
    public Mono<ResponseEntity<Map<String, String>>> logout(
            @RequestBody(required = false) RefreshTokenRequest request) {

        // For stateless JWT setups, "logout" can be purely client-side (delete token).
        // If a refresh token is provided, we also revoke it at Keycloak.
        String refreshToken = request != null ? request.getRefreshToken() : null;
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.just(ResponseEntity.ok(Map.of(
                    "message", "Logged out successfully",
                    "timestamp", Instant.now().toString())));
        }

        return authService.logout(refreshToken)
                .then(Mono.just(ResponseEntity.ok(Map.of(
                        "message", "Logged out successfully",
                        "timestamp", Instant.now().toString()))))
                .onErrorResume(error -> {
                    // Deliberately still returns success to the client (logout is best-effort
                    // revocation for a stateless JWT setup — a Keycloak-side failure shouldn't
                    // block the client from completing logout), but the failure is now logged
                    // so a persistently-failing revocation (e.g. misconfiguration leaving
                    // refresh tokens valid indefinitely after "successful" logout) is visible
                    // to operators instead of a silent blind spot.
                    log.warn("Failed to revoke refresh token at Keycloak during logout", error);
                    return Mono.just(ResponseEntity.ok(Map.of(
                            "message", "Logout completed",
                            "timestamp", Instant.now().toString())));
                });
    }

    /**
     * PUBLIC - Get OpenID Connect configuration.
     *
     * <p>Frontend example: {@code GET /api/auth/config}
     */
    @GetMapping("/config")
    public Mono<ResponseEntity<Map<String, Object>>> getConfig() {
        return authService.getOpenIdConfiguration()
                .map(ResponseEntity::ok);
    }

    /**
     * PUBLIC - Error endpoint (OAuth failure redirect target).
     */
    @GetMapping("/error")
    public ResponseEntity<ApiError> handleError(
            @RequestParam(required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription,
            HttpServletRequest request) {

        String errorCode = error != null ? error : "AUTHENTICATION_FAILED";

        ApiError response = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(error != null ? error : "authentication_failed")
                .errorCode(errorCode)
                .message(errorDescription != null ? errorDescription : "Authentication failed")
                .path(request.getRequestURI())
                .correlationId(CorrelationIdUtils.getCurrent())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Extracts and validates the Bearer token from an Authorization header.
     *
     * @param authHeader the raw {@code Authorization} header value
     * @return the token portion following the {@code Bearer } prefix, trimmed
     * @throws ResponseStatusException 400 if the header is missing or does not use the Bearer
     *                                  scheme (a plain {@code String.replace} on a header that
     *                                  doesn't contain "Bearer " is a silent no-op, which
     *                                  previously let a malformed header pass straight through
     *                                  as if it were the token)
     */
    private String extractBearerToken(String authHeader) {
        if (authHeader == null
                || !authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Authorization header must use the Bearer scheme");
        }
        return authHeader.substring(BEARER_PREFIX.length()).trim();
    }

    /**
     * Resolves the effective redirect URI (falling back to the configured default when the
     * caller omits one) and validates it against {@code app.security.allowed-redirect-uris}.
     *
     * <p>Matching compares the parsed origin (scheme + host + port) of the candidate against
     * each allowed entry — never a string {@code startsWith} check, which a hostname like
     * {@code https://app.example.com.evil.com} would defeat. When no allow-list is configured,
     * only the configured default redirect URI's origin is accepted, so this fails closed
     * rather than open in an environment where {@code ALLOWED_ORIGINS} isn't set.
     *
     * @throws ResponseStatusException 400 if the resolved redirect URI's origin is not allowed
     */
    private String resolveAndValidateRedirectUri(String redirectUri) {
        String defaultRedirectUri = appProperties.getSecurity().getDefaultRedirectUri();
        String resolved = (redirectUri == null || redirectUri.isBlank()) ? defaultRedirectUri : redirectUri;

        if (resolved == null || resolved.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "redirectUri is required");
        }

        List<String> allowedOrigins = appProperties.getSecurity().getAllowedRedirectUris();
        boolean allowedByList = allowedOrigins != null
                && allowedOrigins.stream().anyMatch(origin -> sameOrigin(resolved, origin));
        boolean allowedByDefault = defaultRedirectUri != null && sameOrigin(resolved, defaultRedirectUri);

        if (!allowedByList && !allowedByDefault) {
            log.warn("Rejected redirectUri outside the configured allow-list");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "redirectUri is not an allowed redirect target");
        }

        return resolved;
    }

    private boolean sameOrigin(String candidateUri, String allowedUri) {
        try {
            URI candidate = URI.create(candidateUri);
            URI allowed = URI.create(allowedUri);
            return Objects.equals(candidate.getScheme(), allowed.getScheme())
                    && Objects.equals(candidate.getHost(), allowed.getHost())
                    && candidate.getPort() == allowed.getPort();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
