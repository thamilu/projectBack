package com.eshop.app.service.auth;

import com.eshop.app.config.KeycloakConfig;
import com.eshop.app.dto.auth.*;
import com.eshop.app.exception.KeycloakException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAuthService {
    
    private final WebClient webClient;
    private final KeycloakConfig keycloakConfig;
    
    /**
     * Login with username and password (Resource Owner Password Credentials)
     */
    public Mono<TokenResponse> login(LoginRequest request) {
        log.info("Attempting login for user: {}", request.getUsername());
        
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", keycloakConfig.getClientId());
        formData.add("client_secret", keycloakConfig.getClientSecret());
        formData.add("username", request.getUsername());
        formData.add("password", request.getPassword());
        formData.add("scope", "openid profile email");
        
        return webClient.post()
                .uri(keycloakConfig.getTokenEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(
                    status -> status.is4xxClientError() || status.is5xxServerError(),
                    response -> response.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.error("Login failed for user {}: {}", request.getUsername(), body);
                            return Mono.error(new KeycloakException(
                                "Authentication failed: Invalid username or password", 
                                HttpStatus.UNAUTHORIZED
                            ));
                        })
                )
                .bodyToMono(TokenResponse.class)
                .doOnSuccess(token -> log.info("Login successful for user: {}", request.getUsername()))
                .doOnError(error -> log.error("Login error for user {}: {}", 
                                             request.getUsername(), error.getMessage()));
    }
    
    /**
     * Refresh access token using refresh token
     */
    public Mono<TokenResponse> refreshToken(RefreshTokenRequest request) {
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
                .onStatus(
                    status -> status.isError(),
                    response -> response.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new KeycloakException(
                            "Token refresh failed", 
                            HttpStatus.UNAUTHORIZED
                        )))
                )
                .bodyToMono(TokenResponse.class)
                .doOnSuccess(token -> log.info("Token refreshed successfully"))
                .doOnError(error -> log.error("Token refresh failed: {}", error.getMessage()));
    }
    
    /**
     * Exchange authorization code for tokens (Authorization Code Flow)
     */
    public Mono<TokenResponse> exchangeAuthorizationCode(String code, String redirectUri) {
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
                .onStatus(
                    status -> status.isError(),
                    response -> response.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new KeycloakException(
                            "Authorization code exchange failed", 
                            HttpStatus.BAD_REQUEST
                        )))
                )
                .bodyToMono(TokenResponse.class)
                .doOnSuccess(token -> log.info("Authorization code exchanged successfully"));
    }
    
    /**
     * Get user information from access token
     */
    public Mono<UserInfoResponse> getUserInfo(String accessToken) {
        log.info("Fetching user info");
        
        return webClient.get()
                .uri(keycloakConfig.getUserInfoEndpoint())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .onStatus(
                    status -> status.isError(),
                    response -> Mono.error(new KeycloakException(
                        "Failed to fetch user info", 
                        HttpStatus.UNAUTHORIZED
                    ))
                )
                .bodyToMono(UserInfoResponse.class)
                .doOnSuccess(userInfo -> log.info("User info fetched: {}", userInfo.getPreferredUsername()));
    }
    
    /**
     * Introspect token (validate and get token details)
     */
    public Mono<Map<String, Object>> introspectToken(String token) {
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
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .doOnSuccess(result -> log.info("Token introspection result: active={}", 
                               result.get("active")));
    }
    
    /**
     * Logout user (invalidate refresh token)
     */
    public Mono<Void> logout(String refreshToken) {
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
                .doOnSuccess(v -> log.info("Logout successful"))
                .doOnError(error -> log.error("Logout failed: {}", error.getMessage()));
    }
    
    /**
     * Get OpenID Connect configuration
     */
    public Mono<Map<String, Object>> getOpenIdConfiguration() {
        log.info("Fetching OpenID configuration");
        
        return webClient.get()
            .uri(keycloakConfig.getWellKnownEndpoint())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }
    
    /**
     * Get JWK Set (public keys for JWT validation)
     */
    public Mono<Map<String, Object>> getJwkSet() {
        log.info("Fetching JWK Set");
        
        return webClient.get()
            .uri(keycloakConfig.getCertsEndpoint())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }
    
    /**
     * Generate authorization URL for OAuth2 login
     */
    public String getAuthorizationUrl(String redirectUri, String state) {
        if (state == null || state.isEmpty()) {
            state = UUID.randomUUID().toString();
        }
        
        return String.format(
            "%s?client_id=%s&redirect_uri=%s&response_type=code&scope=openid%%20profile%%20email&state=%s",
            keycloakConfig.getAuthorizationEndpoint(),
            keycloakConfig.getClientId(),
            redirectUri,
            state
        );
    }
    
    /**
     * Get client credentials token (for service-to-service communication)
     */
    public Mono<TokenResponse> getClientCredentialsToken() {
        log.info("Requesting client credentials token");
        
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
                .doOnSuccess(token -> log.info("Client credentials token obtained"));
    }
}
