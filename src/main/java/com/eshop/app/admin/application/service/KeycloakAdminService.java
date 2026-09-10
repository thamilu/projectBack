package com.eshop.app.admin.application.service;

import com.eshop.app.admin.api.request.AdminUserUpdateRequest;
import com.eshop.app.core.infrastructure.config.security.keycloak.KeycloakConfig;
import com.eshop.app.user.api.request.RegisterRequest;
import com.eshop.app.user.api.response.TokenResponse;
import com.eshop.app.core.exception.infrastructure.KeycloakException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminService {

    // Safety margin subtracted from the token's real expiry so a cached token is never
    // handed out with only a few seconds of life left, avoiding a race where an in-flight
    // Keycloak Admin API call fails auth mid-request.
    private static final long TOKEN_EXPIRY_BUFFER_SECONDS = 15L;
    // Keycloak's default access token TTL is 5 minutes; used only if the token response
    // omits expires_in, which the OAuth2 spec does not guarantee.
    private static final long DEFAULT_TOKEN_TTL_SECONDS = 60L;

    private final WebClient webClient;
    private final KeycloakConfig keycloakConfig;

    /**
     * Cached admin access token, shared across all callers of this singleton service.
     *
     * <p>PERFORMANCE FIX: previously, every single admin operation (list users, look up
     * one user, delete, reset password, update) performed its own master-realm
     * resource-owner-password-credentials grant against Keycloak's token endpoint before
     * doing any actual work — meaning admin API throughput was bottlenecked by, and
     * directly proportional to, Keycloak's token-endpoint latency, and Keycloak's token
     * endpoint absorbed load equal to 2x the admin API's own request volume. Reactor's
     * {@code Mono#cache(ttlForValue, ttlForError, ttlForEmpty)} caches the successful
     * token for (expires_in - buffer) and transparently re-fetches after expiry; failed
     * fetches are never cached (ttlForError = ZERO), so a transient Keycloak outage does
     * not poison the cache for its recovery window. The TTL is derived per-value from the
     * emitted {@link AdminTokenHolder#expiresInSeconds()}, which is why the token is
     * wrapped in a holder record rather than cached as a bare String.</p>
     */
    private final Mono<AdminTokenHolder> adminTokenCache = Mono.defer(this::fetchAdminTokenHolder)
            .cache(holder -> Duration.ofSeconds(Math.max(holder.expiresInSeconds() - TOKEN_EXPIRY_BUFFER_SECONDS, 5L)),
                    throwable -> Duration.ZERO,
                    () -> Duration.ZERO);

    private record AdminTokenHolder(String accessToken, long expiresInSeconds) {}

    /**
     * Get a cached (or freshly fetched, if expired/absent) admin access token.
     */
    private Mono<String> getAdminToken() {
        return adminTokenCache.map(AdminTokenHolder::accessToken);
    }

    private Mono<AdminTokenHolder> fetchAdminTokenHolder() {
        log.debug("Requesting admin token (cache miss or expired)");

        // eshop-admin-backend is a confidential service-account client scoped to the
        // eshop-admin realm (see keycloak-import/eshop-admin-realm.json), not a client
        // in Keycloak's own "master" realm — and it has directAccessGrantsEnabled=false,
        // so a "password" grant is rejected. Match KeycloakAdminClientConfig: authenticate
        // via client_credentials against the eshop-admin realm.
        String tokenUrl = String.format(
            "%s/realms/%s/protocol/openid-connect/token",
            keycloakConfig.getAuthServerUrl(),
            keycloakConfig.getAdminRealm()
        );

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", keycloakConfig.getAdminClientId());
        formData.add("client_secret", keycloakConfig.getAdminClientSecret());

        return webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .map(response -> new AdminTokenHolder(
                        response.getAccessToken(),
                        response.getExpiresIn() != null ? response.getExpiresIn() : DEFAULT_TOKEN_TTL_SECONDS))
                .doOnSuccess(holder -> log.debug("Admin token obtained, ttl={}s", holder.expiresInSeconds()))
                .doOnError(error -> log.error("Failed to get admin token: {}", error.getMessage()));
    }

    /**
     * Create a new user
     */
    public Mono<Map<String, String>> createUser(RegisterRequest request) {
        log.info("Creating user with email: {}", request.getEmail());

        return getAdminToken().flatMap(adminToken -> {
            Map<String, Object> userData = new HashMap<>();
            userData.put("username", request.getEmail());
            userData.put("email", request.getEmail());
            userData.put("enabled", request.getEnabled());
            userData.put("emailVerified", false);

            if (request.getFirstName() != null || request.getLastName() != null) {
                userData.put("firstName", request.getFirstName());
                userData.put("lastName", request.getLastName());
            }

            // Set password
            Map<String, Object> credential = new HashMap<>();
            credential.put("type", "password");
            credential.put("value", request.getPassword());
            credential.put("temporary", false);
            userData.put("credentials", List.of(credential));

            return webClient.post()
                    .uri(keycloakConfig.getAdminUsersEndpoint())
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(userData)
                    .retrieve()
                    .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                            .flatMap(body -> {
                                log.error("User creation failed: {}", body);
                                return Mono.error(new KeycloakException(
                                    "User creation failed: " + body,
                                    HttpStatus.BAD_REQUEST
                                ));
                            })
                    )
                    .bodyToMono(Void.class)
                    .then(getUserByEmail(request.getEmail()))
                    .map(userMap -> {
                        String userId = (String) userMap.get("id");
                        log.info("User created and ID resolved: {} -> {}", request.getEmail(), userId);
                        return Map.of(
                                "message", "User created successfully",
                                "email", request.getEmail(),
                                "id", userId);
                    });
        });
    }

    /**
     * Get all users, paginated via Keycloak's native first/max offset pagination.
     *
     * @param page zero-based page index
     * @param size page size
     */
    public Mono<List<Map<String, Object>>> getAllUsers(int page, int size) {
        log.info("Fetching users (page={}, size={})", page, size);

        int first = page * size;

        // The shared WebClient bean has no configured baseUrl (all Keycloak endpoints are
        // absolute URLs), so the URI must be built from the absolute base via
        // UriComponentsBuilder.fromUriString(...) rather than the relative uriBuilder
        // lambda form (which would misinterpret an absolute URL string as a path segment).
        java.net.URI uri = UriComponentsBuilder.fromUriString(keycloakConfig.getAdminUsersEndpoint())
                .queryParam("first", first)
                .queryParam("max", size)
                .build()
                .encode()
                .toUri();

        return getAdminToken().flatMap(adminToken ->
            webClient.get()
                .uri(uri)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .doOnSuccess(users -> log.info("Fetched {} users (page={}, size={})", users.size(), page, size))
        );
    }

    /**
     * Get user by email
     */
    public Mono<Map<String, Object>> getUserByEmail(String email) {
        log.info("Fetching user by email");

        // Built via UriComponentsBuilder + encode() (not string concatenation) so the email
        // value is correctly percent-encoded — e.g. "+" in "john+test@example.com" must be
        // encoded to "%2B" or Keycloak/the HTTP layer would otherwise interpret it as a
        // literal space in the query string, and any accidental "&"/control characters
        // cannot corrupt or inject into the query.
        java.net.URI uri = UriComponentsBuilder.fromUriString(keycloakConfig.getAdminUsersEndpoint())
                .queryParam("email", email)
                .build()
                .encode()
                .toUri();

        return getAdminToken().flatMap(adminToken ->
            webClient.get()
                .uri(uri)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .map(list -> list == null || list.isEmpty() ? null : list.get(0))
                .doOnSuccess(user -> log.info("User lookup by email completed, found={}", user != null))
        );
    }

    /**
     * Delete user
     */
    public Mono<Map<String, String>> deleteUser(String userId) {
        log.info("Deleting user: {}", userId);

        return getAdminToken().flatMap(adminToken ->
            webClient.delete()
                    .uri(keycloakConfig.getAdminUserEndpoint(userId))
                    .header("Authorization", "Bearer " + adminToken)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .then(Mono.just(Map.of(
                        "message", "User deleted successfully",
                        "userId", userId
                    )))
                    .doOnSuccess(result -> log.info("User deleted: {}", userId))
        );
    }

    /**
     * Reset user password
     */
    public Mono<Map<String, String>> resetPassword(String userId, String newPassword, boolean temporary) {
        log.info("Resetting password for user: {}", userId);

        return getAdminToken().flatMap(adminToken -> {
            Map<String, Object> credential = new HashMap<>();
            credential.put("type", "password");
            credential.put("value", newPassword);
            credential.put("temporary", temporary);

            return webClient.put()
                    .uri(keycloakConfig.getAdminResetPasswordEndpoint(userId))
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(credential)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .then(Mono.just(Map.of(
                        "message", "Password reset successfully",
                        "userId", userId
                    )))
                    .doOnSuccess(result -> log.info("Password reset for user: {}", userId));
        });
    }

    /**
     * Update user. Accepts an explicit, allow-listed DTO instead of an open map (see
     * {@link AdminUserUpdateRequest} javadoc) — only the fields the caller actually set
     * (non-null) are forwarded to Keycloak, supporting partial updates.
     */
    public Mono<Map<String, String>> updateUser(String userId, AdminUserUpdateRequest updates) {
        log.info("Updating user: {}", userId);

        Map<String, Object> allowedFields = new HashMap<>();
        if (updates.getFirstName() != null) {
            allowedFields.put("firstName", updates.getFirstName());
        }
        if (updates.getLastName() != null) {
            allowedFields.put("lastName", updates.getLastName());
        }
        if (updates.getEnabled() != null) {
            allowedFields.put("enabled", updates.getEnabled());
        }
        if (updates.getEmailVerified() != null) {
            allowedFields.put("emailVerified", updates.getEmailVerified());
        }

        if (allowedFields.isEmpty()) {
            return Mono.just(Map.of(
                    "message", "No fields to update",
                    "userId", userId));
        }

        return getAdminToken().flatMap(adminToken ->
            webClient.put()
                    .uri(keycloakConfig.getAdminUserEndpoint(userId))
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(allowedFields)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .then(Mono.just(Map.of(
                        "message", "User updated successfully",
                        "userId", userId
                    )))
                    .doOnSuccess(result -> log.info("User updated: {}", userId))
        );
    }
}
