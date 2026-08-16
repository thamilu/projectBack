package com.eshop.app.user.api.controller;

import com.eshop.app.user.application.security.JwtClaimExtractor;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JWT User Identity Controller
 *
 * <p>
 * Exposes authenticated user information derived from the validated JWT token.
 * </p>
 *
 * <p>
 * <strong>Use Cases:</strong>
 * </p>
 * <ul>
 * <li>Debugging JWT token claims (non-sensitive subset only — see SECURITY note)</li>
 * <li>User profile information</li>
 * <li>Multi-tenant logic</li>
 * <li>Frontend user context</li>
 * </ul>
 *
 * <p>
 * <strong>SECURITY NOTE:</strong> This endpoint intentionally does not return the raw,
 * complete JWT claim set. Keycloak-issued tokens commonly include claims such as
 * {@code resource_access} that enumerate other registered Keycloak clients/services and
 * this user's role grants against them — dumping those here would leak internal service
 * topology to any authenticated caller. Only an explicit allow-list is returned.
 * </p>
 *
 * @author EShop Team
 * @version 2.1
 * @since 2026-01-01
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@io.swagger.v3.oas.annotations.tags.Tag(name = "User Identity", description = "JWT-based user authentication and identity endpoints")
public class MeController {

    private final JwtClaimExtractor jwtClaimExtractor;

    /**
     * Get Current Authenticated User from JWT
     *
     * <p>
     * <strong>Usage Examples:</strong>
     * </p>
     *
     * <pre>
     * // Frontend (React/Next.js)
     * fetch('/api/v1/me', {
     *   headers: { 'Authorization': 'Bearer ' + accessToken }
     * })
     *
     * // cURL
     * curl -H "Authorization: Bearer YOUR_JWT_TOKEN" {BASE_URL}/api/v1/me
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
     *   "tokenIssuedAt": "2026-01-01T00:00:00Z",
     *   "tokenExpiresAt": "2026-01-01T01:00:00Z"
     * }
     * </pre>
     *
     * @param jwt            the validated JWT token from Keycloak (injected by Spring Security)
     * @param authentication the current Authentication, used to derive granted authorities
     * @return identity information derived from an explicit, reviewed claim allow-list
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Timed(value = "user.me.jwt.get", description = "Time to get current user identity from JWT")
    @Operation(summary = "Get Current User Identity", description = "Returns authenticated user information derived from an explicit, reviewed subset of validated JWT claims.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User identity retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token")
    })
    public ResponseEntity<Map<String, Object>> me(
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        // Defensive guard in case this endpoint is ever reached without authentication
        // (e.g. a future SecurityFilterChain regression) — returns a clean 401 instead of
        // an unhandled NullPointerException.
        if (jwt == null || authentication == null) {
            log.warn("MeController.me() invoked without a resolved JWT/Authentication - returning 401. "
                    + "Verify SecurityFilterChain coverage for /api/v1/me.");
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // JwtClaimExtractor.extractEffectiveRoles is the same canonical extraction used by
        // SecurityConfig#jwtAuthenticationConverter to build this user's real
        // GrantedAuthority set (realm + client-scoped + root + groups claims merged), so
        // this field always matches actual authorization behavior rather than a
        // separately-maintained (and easily divergent) reimplementation.
        List<String> roles = jwtClaimExtractor.extractEffectiveRoles(jwt);

        // Map.of(...) throws NullPointerException on any null key/value; sub/iat/exp are
        // not guaranteed non-null by the JWT spec, so a plain, null-tolerant map is used
        // instead, with every potentially-null claim explicitly guarded.
        Map<String, Object> body = new LinkedHashMap<>();
        String subject = jwt.getSubject();
        body.put("sub", subject);
        body.put("userId", subject);
        body.put("username", jwt.getClaimAsString("preferred_username") != null
                ? jwt.getClaimAsString("preferred_username")
                : subject);
        body.put("email", jwt.getClaimAsString("email") != null ? jwt.getClaimAsString("email") : "");
        body.put("roles", roles);
        body.put("authorities", authorities);

        Instant issuedAt = jwt.getIssuedAt();
        Instant expiresAt = jwt.getExpiresAt();
        body.put("tokenIssuedAt", issuedAt);
        body.put("tokenExpiresAt", expiresAt);

        // 'allClaims' (raw jwt.getClaims()) intentionally removed: it exposed the full,
        // unfiltered token claim set, including 'resource_access' entries that enumerate
        // OTHER backend services/clients in the Keycloak realm and this user's role grants
        // against them. Only the explicit allow-list above is returned.

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(body);
    }
}
