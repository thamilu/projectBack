package com.eshop.app.user.application.service;

import com.eshop.app.core.infrastructure.config.security.keycloak.KeycloakConfig;
import com.eshop.app.user.api.request.LoginRequest;
import com.eshop.app.user.api.request.RefreshTokenRequest;
import com.eshop.app.user.api.response.TokenResponse;
import com.eshop.app.user.api.response.UserInfoResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;

import static com.eshop.app.user.application.service.KeycloakAuthHelper.Operation.AUTHORIZATION_CODE;
import static com.eshop.app.user.application.service.KeycloakAuthHelper.Operation.CLIENT_CREDENTIALS;
import static com.eshop.app.user.application.service.KeycloakAuthHelper.Operation.INTROSPECT;
import static com.eshop.app.user.application.service.KeycloakAuthHelper.Operation.JWKS;
import static com.eshop.app.user.application.service.KeycloakAuthHelper.Operation.LOGOUT;
import static com.eshop.app.user.application.service.KeycloakAuthHelper.Operation.OPENID_CONFIGURATION;
import static com.eshop.app.user.application.service.KeycloakAuthHelper.Operation.PASSWORD_GRANT;
import static com.eshop.app.user.application.service.KeycloakAuthHelper.Operation.REFRESH_TOKEN;
import static com.eshop.app.user.application.service.KeycloakAuthHelper.Operation.USER_INFO;

/**
 * Keycloak Authentication Service
 *
 * <p><strong>ARCHITECTURE NOTE:</strong> {@link #login} uses the OAuth2 Resource Owner
 * Password Credentials (ROPC) grant, which requires this backend to directly receive and
 * forward the user's plaintext password to Keycloak. ROPC is deprecated in OAuth 2.1 and
 * is incompatible with IdP-level MFA/passwordless/social login — Keycloak's step-up and
 * alternative-authenticator flows are built around browser-redirect-based authentication
 * (Authorization Code Flow), which {@link #getAuthorizationUrl}/
 * {@link #exchangeAuthorizationCode} already implement in this same class. Continued use
 * of ROPC for the primary login path should be treated as an explicit, time-boxed
 * accepted risk (e.g. legacy client compatibility) or scheduled for migration to
 * Authorization Code Flow + PKCE — this is a product/security decision, not something a
 * code-level change alone can resolve.</p>
 *
 * <p>Every outbound call shares one timeout ({@link KeycloakConfig#getTimeout()}) and one
 * error-mapping path: {@code WebClient#retrieve()} already throws
 * {@code WebClientResponseException} for any 4xx/5xx response with no need for a custom
 * {@code onStatus(...)} handler, so each chain ends with
 * {@code .onErrorMap(error -> authHelper.mapToKeycloakException(error, OPERATION))},
 * which converts that (or a timeout, or a future circuit-breaker rejection) into a
 * {@link com.eshop.app.core.exception.infrastructure.KeycloakException} carrying
 * Keycloak's real HTTP status and an operation-aware, curated message. This matters
 * because {@code AuthControllerExceptionHandler} branches on
 * {@code status.is5xxServerError()} to redact infrastructure-failure detail behind a
 * generic "temporarily unavailable" message while preserving curated 4xx messages — that
 * logic only works correctly if the status embedded here reflects what Keycloak actually
 * returned.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAuthService {

    private final WebClient webClient;
    private final KeycloakConfig keycloakConfig;
    private final KeycloakAuthHelper authHelper;

    // Client-credentials tokens are meant to be reused until near expiry, not re-requested
    // per call — this mirrors the admin-token caching pattern in KeycloakAdminService.
    // ttlForError = ZERO so a failed fetch is never cached (a transient outage doesn't
    // poison the cache for its own recovery window).
    private static final long TOKEN_EXPIRY_BUFFER_SECONDS = 15L;
    private static final long DEFAULT_TOKEN_TTL_SECONDS = 60L;

    private record CachedToken(TokenResponse response, long expiresInSeconds) {}

    private final Mono<CachedToken> cachedClientCredentialsToken = Mono.defer(this::fetchClientCredentialsToken)
            .cache(cached -> Duration.ofSeconds(Math.max(cached.expiresInSeconds() - TOKEN_EXPIRY_BUFFER_SECONDS, 5L)),
                    throwable -> Duration.ZERO,
                    () -> Duration.ZERO);

    private final Mono<Map<String, Object>> cachedJwkSet = Mono.defer(this::fetchJwkSet)
            .cache(Duration.ofMinutes(10));

    /**
     * Login with username and password (Resource Owner Password Credentials).
     * See class-level ARCHITECTURE NOTE regarding ROPC usage.
     */
    public Mono<TokenResponse> login(LoginRequest request) {
        authHelper.validateLoginRequest(request);
        log.info("Attempting login for user: {}", authHelper.maskEmail(request.getEmail()));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", keycloakConfig.getClientId());
        formData.add("client_secret", keycloakConfig.getClientSecret());
        formData.add("username", request.getEmail());
        formData.add("password", request.getPassword());
        formData.add("scope", "openid profile email");

        return webClient.post()
                .uri(keycloakConfig.getTokenEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .timeout(keycloakConfig.getTimeout())
                .onErrorMap(error -> authHelper.mapToKeycloakException(error, PASSWORD_GRANT))
                .doOnSuccess(token -> log.info("Login successful for user: {}", authHelper.maskEmail(request.getEmail())))
                .doOnError(error -> log.error("Login error for user {}: {}",
                        authHelper.maskEmail(request.getEmail()), error.getMessage()));
    }

    /**
     * Refresh access token using refresh token
     */
    public Mono<TokenResponse> refreshToken(RefreshTokenRequest request) {
        authHelper.validateRefreshTokenRequest(request);
        log.info("Refreshing access token");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", keycloakConfig.getClientId());
        formData.add("client_secret", keycloakConfig.getClientSecret());
        formData.add("refresh_token", request.getRefreshToken());

        return webClient.post()
                .uri(keycloakConfig.getTokenEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .timeout(keycloakConfig.getTimeout())
                .onErrorMap(error -> authHelper.mapToKeycloakException(error, REFRESH_TOKEN))
                .doOnSuccess(token -> log.info("Token refreshed successfully"))
                .doOnError(error -> log.error("Token refresh failed: {}", error.getMessage()));
    }

    /**
     * Exchange authorization code for tokens (Authorization Code Flow)
     */
    public Mono<TokenResponse> exchangeAuthorizationCode(String code, String redirectUri) {
        authHelper.validateAuthorizationCodeRequest(code, redirectUri);
        log.info("Exchanging authorization code for tokens");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", keycloakConfig.getClientId());
        formData.add("client_secret", keycloakConfig.getClientSecret());
        formData.add("code", code);
        formData.add("redirect_uri", redirectUri);

        return webClient.post()
                .uri(keycloakConfig.getTokenEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .timeout(keycloakConfig.getTimeout())
                .onErrorMap(error -> authHelper.mapToKeycloakException(error, AUTHORIZATION_CODE))
                .doOnSuccess(token -> log.info("Authorization code exchanged successfully"));
    }

    /**
     * Get user information from access token
     */
    public Mono<UserInfoResponse> getUserInfo(String accessToken) {
        authHelper.validateToken(accessToken);
        log.info("Fetching user info");

        return webClient.get()
                .uri(keycloakConfig.getUserInfoEndpoint())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(UserInfoResponse.class)
                .timeout(keycloakConfig.getTimeout())
                .onErrorMap(error -> authHelper.mapToKeycloakException(error, USER_INFO))
                .doOnSuccess(userInfo -> log.info("User info fetched: {}", authHelper.maskEmail(userInfo.email())));
    }

    /**
     * Introspect token (validate and get token details)
     */
    public Mono<Map<String, Object>> introspectToken(String token) {
        authHelper.validateToken(token);
        log.info("Introspecting token");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", keycloakConfig.getClientId());
        formData.add("client_secret", keycloakConfig.getClientSecret());
        formData.add("token", token);

        return webClient.post()
                .uri(keycloakConfig.getIntrospectEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() { })
                .timeout(keycloakConfig.getTimeout())
                .onErrorMap(error -> authHelper.mapToKeycloakException(error, INTROSPECT))
                .doOnSuccess(result -> log.info("Token introspect result: active={}", result.get("active")));
    }

    /**
     * Logout user (invalidate refresh token).
     *
     * <p>Note: {@code KeycloakAuthController#logout} already treats ANY failure from this
     * method as a benign, best-effort outcome (logout is stateless-JWT client-side by
     * design; a Keycloak-side revocation failure — including an already-invalidated
     * refresh token, which Keycloak reports as a 400 {@code invalid_grant} — must not
     * block the client from completing logout). That safety net lives at the controller,
     * so this method simply maps errors consistently rather than special-casing
     * {@code invalid_grant} itself.</p>
     */
    public Mono<Void> logout(String refreshToken) {
        authHelper.validateToken(refreshToken);
        log.info("Logging out user");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", keycloakConfig.getClientId());
        formData.add("client_secret", keycloakConfig.getClientSecret());
        formData.add("refresh_token", refreshToken);

        return webClient.post()
                .uri(keycloakConfig.getLogoutEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(keycloakConfig.getTimeout())
                .onErrorMap(error -> authHelper.mapToKeycloakException(error, LOGOUT))
                .doOnSuccess(v -> log.info("Logout successful"))
                .doOnError(error -> log.error("Logout failed: {}", error.getMessage()));
    }

    /**
     * Get OpenID Connect configuration.
     *
     * <p>Deliberately NOT cached, unlike {@link #getJwkSet()}/
     * {@link #getClientCredentialsToken()}: {@code KeycloakConnectivityHealthIndicator}
     * calls this method specifically to verify live Keycloak reachability. A cached
     * success response would report the health check as "up" using stale data even while
     * Keycloak is actually down, defeating the purpose of a liveness check. If a cached,
     * public-facing variant of this data is needed later (e.g. for {@code GET
     * /api/auth/config}), add a separate cached accessor rather than caching this one.
     */
    public Mono<Map<String, Object>> getOpenIdConfiguration() {
        log.info("Fetching OpenID configuration");

        return webClient.get()
                .uri(keycloakConfig.getWellKnownEndpoint())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() { })
                .timeout(keycloakConfig.getTimeout())
                .onErrorMap(error -> authHelper.mapToKeycloakException(error, OPENID_CONFIGURATION));
    }

    /**
     * Get JWK Set (public keys for JWT validation). Cached with a conservative TTL: key
     * rotation is infrequent, and this data is not used by any liveness check (unlike
     * {@link #getOpenIdConfiguration()}), so caching carries no correctness risk here.
     */
    public Mono<Map<String, Object>> getJwkSet() {
        return cachedJwkSet;
    }

    private Mono<Map<String, Object>> fetchJwkSet() {
        log.info("Fetching JWK Set (cache miss or expired)");

        return webClient.get()
                .uri(keycloakConfig.getCertsEndpoint())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() { })
                .timeout(keycloakConfig.getTimeout())
                .onErrorMap(error -> authHelper.mapToKeycloakException(error, JWKS));
    }

    /**
     * Generate authorization URL for OAuth2 login.
     *
     * <p>{@code redirectUri} and {@code state} are URL-encoded before being interpolated
     * into the query string. Without this, a {@code redirectUri} containing its own query
     * parameters (a common real-world pattern, e.g.
     * {@code "https://app.example.com/callback?flow=checkout"}) would corrupt the
     * resulting URL's parameter boundaries. Note: {@code KeycloakAuthController#getLoginUrl}
     * already validates {@code redirectUri} against an origin allow-list and always
     * generates {@code state} itself as a random UUID before calling this method, so
     * neither value is attacker-controlled on the actual call path — this fix is a
     * correctness/robustness guarantee for this method regardless of caller, not a
     * response to an exploitable path in the current codebase.</p>
     */
    public String getAuthorizationUrl(String redirectUri, String state) {
        Assert.hasText(redirectUri, "redirectUri must not be blank");

        if (state == null || state.isEmpty()) {
            state = UUID.randomUUID().toString();
        }

        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        String encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8);
        String encodedClientId = URLEncoder.encode(keycloakConfig.getClientId(), StandardCharsets.UTF_8);

        return String.format(
                "%s?client_id=%s&redirect_uri=%s&response_type=code&scope=openid%%20profile%%20email&state=%s",
                keycloakConfig.getAuthorizationEndpoint(),
                encodedClientId,
                encodedRedirectUri,
                encodedState);
    }

    /**
     * Get client credentials token (for service-to-service communication). Cached and
     * reused until near expiry, mirroring {@code KeycloakAdminService}'s admin-token
     * caching — a fresh token was previously requested on every single call.
     */
    public Mono<TokenResponse> getClientCredentialsToken() {
        return cachedClientCredentialsToken.map(CachedToken::response);
    }

    private Mono<CachedToken> fetchClientCredentialsToken() {
        log.info("Requesting client credentials token (cache miss or expired)");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", keycloakConfig.getClientId());
        formData.add("client_secret", keycloakConfig.getClientSecret());

        return webClient.post()
                .uri(keycloakConfig.getTokenEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .timeout(keycloakConfig.getTimeout())
                .onErrorMap(error -> authHelper.mapToKeycloakException(error, CLIENT_CREDENTIALS))
                .map(response -> new CachedToken(response,
                        response.getExpiresIn() != null ? response.getExpiresIn() : DEFAULT_TOKEN_TTL_SECONDS))
                .doOnSuccess(cached -> log.info("Client credentials token obtained, ttl={}s", cached.expiresInSeconds()));
    }
}
