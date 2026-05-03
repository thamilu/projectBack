package com.eshop.app.controller.auth;

import com.eshop.app.config.properties.AppProperties;
import com.eshop.app.constants.ApiConstants;
import com.eshop.app.dto.auth.*;
import com.eshop.app.service.auth.KeycloakAuthService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({ "/api/auth", ApiConstants.Endpoints.AUTH })
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(originPatterns = "*")
@Tag(name = "Authentication", description = "Keycloak / authentication endpoints")
public class KeycloakAuthController {
    
    private final KeycloakAuthService authService;
    private final AppProperties appProperties;
    
    /**
     * ✅ PUBLIC - Login with username and password
     * 
     * Frontend Example:
     * POST /api/auth/login
     * {
     *   "username": "john.doe",
     *   "password": "password123"
     * }
     */
    @PostMapping("/login")
    @RateLimiter(name = "login")
    public Mono<ResponseEntity<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for user: {}", request.getUsername());
        
        return authService.login(request)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Login failed: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                });
    }
    
    /**
     * ✅ PUBLIC - Refresh access token
     * 
     * Frontend Example:
     * POST /api/auth/refresh
     * {
     *   "refreshToken": "your-refresh-token"
     * }
     */
    @PostMapping("/refresh")
    public Mono<ResponseEntity<TokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request received");
        
        return authService.refreshToken(request)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Token refresh failed: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                });
    }
    
    /**
     * ✅ PUBLIC - Get authorization URL for OAuth2 login
     * 
     * Frontend Example:
     * GET /api/auth/login-url?redirectUri=https://yourapp.com/callback
     * 
     * Response:
     * {
     * "authorizationUrl": "https://keycloak.example.com/realms/master/...",
     * "state": "uuid-here"
     * }
     * 
     * Then redirect user to authorizationUrl in browser
     */
    @GetMapping("/login-url")
    public ResponseEntity<Map<String, String>> getLoginUrl(
            @RequestParam(required = false) String redirectUri) {

        if (redirectUri == null || redirectUri.isBlank()) {
            redirectUri = appProperties.getSecurity().getDefaultRedirectUri();
        }
        
        String state = UUID.randomUUID().toString();
        String authUrl = authService.getAuthorizationUrl(redirectUri, state);
        
        log.info("Generated authorization URL with state: {}", state);
        
        Map<String, String> response = new HashMap<>();
        response.put("authorizationUrl", authUrl);
        response.put("state", state);
        response.put("message", "Redirect user to authorizationUrl");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * ✅ PUBLIC - OAuth2 callback (exchange authorization code for tokens)
     * 
     * Frontend Example:
     * After user logs in via Keycloak, they'll be redirected to:
     * https://yourapp.com/callback?code=xxx&state=yyy
     * 
     * Then call:
     * GET /api/auth/callback?code=xxx&redirectUri=https://yourapp.com/callback
     */
    @GetMapping("/callback")
    public Mono<ResponseEntity<TokenResponse>> handleCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String redirectUri) {

        if (redirectUri == null || redirectUri.isBlank()) {
            redirectUri = appProperties.getSecurity().getDefaultRedirectUri();
        }
        
        log.info("OAuth2 callback received with code");
        
        return authService.exchangeAuthorizationCode(code, redirectUri)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Callback failed: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
                });
    }
    
    /**
     * 🔒 PROTECTED - Get user information
     * 
     * Frontend Example:
     * GET /api/auth/userinfo
     * Headers: Authorization: Bearer <access-token>
     */
    @GetMapping("/userinfo")
    public Mono<ResponseEntity<UserInfoResponse>> getUserInfo(
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.replace("Bearer ", "");
        
        return authService.getUserInfo(token)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> 
                    Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }
    
    /**
     * 🔒 PROTECTED - Get current user from JWT
     * 
     * Frontend Example:
     * GET /api/auth/me
     * Headers: Authorization: Bearer <access-token>
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt) {
        
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("sub", jwt.getSubject());
        userInfo.put("username", jwt.getClaimAsString("preferred_username"));
        userInfo.put("email", jwt.getClaimAsString("email"));
        userInfo.put("name", jwt.getClaimAsString("name"));
        userInfo.put("givenName", jwt.getClaimAsString("given_name"));
        userInfo.put("familyName", jwt.getClaimAsString("family_name"));
        userInfo.put("emailVerified", jwt.getClaimAsBoolean("email_verified"));
        userInfo.put("roles", jwt.getClaimAsStringList("realm_access.roles"));
        userInfo.put("issuedAt", jwt.getIssuedAt());
        userInfo.put("expiresAt", jwt.getExpiresAt());
        
        return ResponseEntity.ok(userInfo);
    }
    
    /**
     * 🔒 PROTECTED - Introspect token (validate token)
     * 
     * Frontend Example:
     * POST /api/auth/introspect
     * Headers: Authorization: Bearer <access-token>
     */
    @PostMapping("/introspect")
    public Mono<ResponseEntity<Map<String, Object>>> introspectToken(
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.replace("Bearer ", "");
        
        return authService.introspectToken(token)
                .map(result -> {
                    Boolean active = (Boolean) result.getOrDefault("active", false);
                    if (active) {
                        return ResponseEntity.ok(result);
                    } else {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
                    }
                });
    }
    
    /**
     * 🔒 PROTECTED - Logout user
     * 
     * Frontend Example:
     * POST /api/auth/logout
     * {
     *   "refreshToken": "your-refresh-token"
     * }
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
                    "timestamp", java.time.Instant.now().toString())));
        }

        return authService.logout(refreshToken)
                .then(Mono.just(ResponseEntity.ok(Map.of(
                    "message", "Logged out successfully",
                    "timestamp", java.time.Instant.now().toString()
                ))))
                .onErrorResume(error -> Mono.just(ResponseEntity.ok(Map.of(
                    "message", "Logout completed",
                    "timestamp", java.time.Instant.now().toString()
                ))));
    }
    
    /**
     * ✅ PUBLIC - Get OpenID Connect configuration
     * 
     * Frontend Example:
     * GET /api/auth/config
     */
    @GetMapping("/config")
    public Mono<ResponseEntity<Map<String, Object>>> getConfig() {
        return authService.getOpenIdConfiguration()
                .map(ResponseEntity::ok);
    }
    
    /**
     * ✅ PUBLIC - Error endpoint
     */
    @GetMapping("/error")
    public ResponseEntity<ErrorResponse> handleError(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description) {
        
        ErrorResponse response = ErrorResponse.builder()
                .status(401)
                .error(error != null ? error : "authentication_failed")
                .message(error_description != null ? error_description : "Authentication failed")
                .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
}
