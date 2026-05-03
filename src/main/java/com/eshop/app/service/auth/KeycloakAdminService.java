package com.eshop.app.service.auth;

import com.eshop.app.config.KeycloakConfig;
import com.eshop.app.dto.auth.RegisterRequest;
import com.eshop.app.dto.auth.TokenResponse;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminService {
    
    private final WebClient webClient;
    private final KeycloakConfig keycloakConfig;
    
    /**
     * Get admin access token
     */
    private Mono<String> getAdminToken() {
        log.info("Requesting admin token");
        
        String tokenUrl = String.format(
            "%s/realms/master/protocol/openid-connect/token",
            keycloakConfig.getAuthServerUrl()
        );
        
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", keycloakConfig.getAdminClientId());
        formData.add("username", keycloakConfig.getAdminUsername());
        formData.add("password", keycloakConfig.getAdminPassword());
        
        return webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .map(TokenResponse::getAccessToken)
                .doOnSuccess(token -> log.info("Admin token obtained"))
                .doOnError(error -> log.error("Failed to get admin token: {}", error.getMessage()));
    }
    
    /**
     * Create a new user
     */
    public Mono<Map<String, String>> createUser(RegisterRequest request) {
        log.info("Creating user: {}", request.getUsername());
        
        return getAdminToken().flatMap(adminToken -> {
            Map<String, Object> userData = new HashMap<>();
            userData.put("username", request.getUsername());
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
                    .then(getUserByUsername(request.getUsername()))
                    .map(userMap -> {
                        String userId = (String) userMap.get("id");
                        log.info("User created and ID resolved: {} -> {}", request.getUsername(), userId);
                        return Map.of(
                                "message", "User created successfully",
                                "username", request.getUsername(),
                                "id", userId);
                    });
        });
    }
    
    /**
     * Get all users
     */
    public Mono<List<Map<String, Object>>> getAllUsers() {
        log.info("Fetching all users");
        
        return getAdminToken().flatMap(adminToken ->
            webClient.get()
                .uri(keycloakConfig.getAdminUsersEndpoint())
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .doOnSuccess(users -> log.info("Fetched {} users", users.size()))
        );
    }
    
    /**
     * Get user by username
     */
    public Mono<Map<String, Object>> getUserByUsername(String username) {
        log.info("Fetching user: {}", username);
        
        return getAdminToken().flatMap(adminToken ->
            webClient.get()
                .uri(keycloakConfig.getAdminUsersEndpoint() + "?username=" + username)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .map(list -> list == null || list.isEmpty() ? null : list.get(0))
                .doOnSuccess(user -> log.info("User found: {}", username))
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
    public Mono<Map<String, String>> resetPassword(String userId, String newPassword) {
        log.info("Resetting password for user: {}", userId);
        
        return getAdminToken().flatMap(adminToken -> {
            Map<String, Object> credential = new HashMap<>();
            credential.put("type", "password");
            credential.put("value", newPassword);
            credential.put("temporary", false);
            
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
     * Update user
     */
    public Mono<Map<String, String>> updateUser(String userId, Map<String, Object> updates) {
        log.info("Updating user: {}", userId);
        
        return getAdminToken().flatMap(adminToken ->
            webClient.put()
                    .uri(keycloakConfig.getAdminUserEndpoint(userId))
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updates)
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
