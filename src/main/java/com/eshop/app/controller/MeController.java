package com.eshop.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JWT User Identity Controller
 * 
 * <p>This controller exposes authenticated user information directly from the JWT token.
 * It validates the Bearer token and returns backend identity information.</p>
 * 
 * <p><strong>Use Cases:</strong></p>
 * <ul>
 *   <li>Debugging JWT token claims</li>
 *   <li>User profile information</li>
 *   <li>Multi-tenant logic</li>
 *   <li>Frontend user context</li>
 * </ul>
 * 
 * @author EShop Team
 * @version 2.0
 * @since 2026-01-01
 */
@RestController
@RequestMapping("/api")
@Tag(name = "User Identity", description = "JWT-based user authentication and identity endpoints")
public class MeController {

    /**
     * Get Current Authenticated User from JWT
     * 
     * <p>
     * This endpoint validates the JWT token from the Authorization header
     * and returns all relevant user claims including roles.
     * </p>
     * 
     * <p>
     * <strong>Usage Examples:</strong>
     * </p>
     * 
     * <pre>
     * // Frontend (React/Next.js)
     * fetch('/api/me', {
     *   headers: { 'Authorization': 'Bearer ' + accessToken }
     * })
     * 
     * // cURL
     * curl -H "Authorization: Bearer YOUR_JWT_TOKEN" {BASE_URL}/api/me
     * 
     * // Postman
     * Authorization → Bearer Token → paste Access Token
     * </pre>
     * 
     * <p>
     * <strong>Response Example:</strong>
     * </p>
     * 
     * <pre>
     * {
     *   "sub": "user-id-12345",
     *   "username": "john@example.com",
     *   "email": "john@example.com",
     *   "roles": ["SELLER", "CUSTOMER"],
     *   "authorities": ["ROLE_SELLER", "ROLE_CUSTOMER"],
     *   "userId": "user-id-12345",
     *   "allClaims": {
     *     "sub": "user-id-12345",
     *     "email": "john@example.com",
     *     "preferred_username": "john@example.com",
     *     "roles": ["SELLER", "CUSTOMER"],
     *     "iat": 1735689600,
     *     "exp": 1735693200
     *   }
     * }
     * </pre>
     * 
     * @param jwt The validated JWT token from Keycloak (injected by Spring
     *            Security)
     * @return Map containing comprehensive user identity information
     */
    @GetMapping("/me")
    @Operation(
        summary = "Get Current User Identity",
        description = "Returns authenticated user information from validated JWT token. Perfect for debugging, user context, and multi-tenant logic.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User identity retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token")
    })
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        
        // Extract granted authorities (roles with ROLE_ prefix)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        
        // Extract raw roles from JWT (without ROLE_ prefix)
        List<String> roles = jwt.getClaimAsStringList("roles");
        
        return Map.of(
            "sub", jwt.getSubject(),
            "userId", jwt.getSubject(),
            "username", jwt.getClaimAsString("preferred_username") != null ? 
                jwt.getClaimAsString("preferred_username") : jwt.getSubject(),
            "email", jwt.getClaimAsString("email") != null ? 
                jwt.getClaimAsString("email") : "",
            "roles", roles != null ? roles : List.of(),
            "authorities", authorities,
            "tokenIssuedAt", jwt.getIssuedAt(),
            "tokenExpiresAt", jwt.getExpiresAt(),
            "allClaims", jwt.getClaims()
        );
    }
}

