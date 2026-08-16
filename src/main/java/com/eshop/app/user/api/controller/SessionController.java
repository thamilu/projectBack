package com.eshop.app.user.api.controller;

import com.eshop.app.user.application.security.JwtClaimExtractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Authentication Session Controller
 *
 * <p><b>CRITICAL-005 FIX:</b> Public endpoint for validating JWT token/session validity
 * without requiring authentication. Solves the chicken-and-egg problem where clients
 * need to check if their token is valid before making authenticated requests.
 *
 * <h2>Problem:</h2>
 * <pre>
 * Authentication failed for request: GET /auth/session
 * </pre>
 *
 * <h2>Root Cause:</h2>
 * <ul>
 *   <li>The /auth/session endpoint required authentication to check if authentication is valid</li>
 *   <li>Circular dependency: can't check token without having valid token</li>
 *   <li>Frontend SPAs need to validate JWT expiry without causing 401 errors</li>
 * </ul>
 *
 * <h2>Solution:</h2>
 * <ul>
 *   <li>Make /auth/session publicly accessible (no authentication required)</li>
 *   <li>Extract and validate JWT from Authorization header manually</li>
 *   <li>Return structured response indicating token status</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>
 * // Frontend code
 * const response = await fetch('/api/v1/auth/session', {
 *   headers: {
 *     'Authorization': `Bearer ${token}`
 *   }
 * });
 * const session = await response.json();
 *
 * if (session.expired || !session.valid) {
 *   // Redirect to login or refresh token
 * }
 * </pre>
 *
 * @author EShop Security Team
 * @version 1.1
 * @since 2025-12-22
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

    private static final String GENERIC_INVALID_MESSAGE = "Invalid or malformed token";

    private final JwtDecoder jwtDecoder;
    private final JwtClaimExtractor jwtClaimExtractor;

    /**
     * Validate current JWT token and return session information.
     *
     * <p>This endpoint is publicly accessible and does not require authentication.
     * It manually extracts and validates the JWT from the Authorization header.
     *
     * @param authHeader the Authorization header (Bearer token)
     * @return session information including validity, expiration, user details
     */
    @GetMapping("/session")
    public ResponseEntity<SessionInfoResponse> getSessionInfo(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // No token provided
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok(SessionInfoResponse.unauthenticated());
        }

        try {
            // Extract token (remove "Bearer " prefix)
            String token = authHeader.substring(7);

            // Decode and validate JWT
            Jwt jwt = jwtDecoder.decode(token);

            // Defensive secondary guard: the default JwtTimestampValidator rejects an
            // already-expired token inside decode() itself (see catch block below), so
            // this branch only fires for decoders that don't validate expiry up front.
            Instant expiration = jwt.getExpiresAt();
            boolean isExpired = expiration != null && Instant.now().isAfter(expiration);

            if (isExpired) {
                return ResponseEntity.ok(SessionInfoResponse.expiredResponse());
            }

            // Extract user details. JwtClaimExtractor.extractEffectiveRoles is the same
            // canonical extraction SecurityConfig#jwtAuthenticationConverter uses to build
            // this user's real GrantedAuthority set, so this field always matches actual
            // authorization behavior rather than a separately-maintained reimplementation.
            String userId = jwt.getSubject();
            List<String> roles = jwtClaimExtractor.extractEffectiveRoles(jwt);

            long secondsRemaining = expiration != null
                ? Math.max(0, Duration.between(Instant.now(), expiration).toSeconds())
                : 0L;

            return ResponseEntity.ok(SessionInfoResponse.authenticatedResponse(
                userId,
                roles,
                expiration,
                secondsRemaining
            ));

        } catch (JwtException e) {
            // Never leak decoder-internal messages (algorithm, issuer, JWKS detail) to an
            // unauthenticated caller; full detail is retained server-side only.
            log.warn("JWT session validation failed: {}", e.getMessage());

            if (isExpiredTokenException(e)) {
                return ResponseEntity.ok(SessionInfoResponse.expiredResponse());
            }
            return ResponseEntity.ok(SessionInfoResponse.invalid(GENERIC_INVALID_MESSAGE));

        } catch (Exception e) {
            // Guarantees the documented "always 200, structured status" contract even for
            // unexpected claim shapes (e.g. IllegalArgumentException from
            // Jwt#getClaimAsStringList when a claim exists but isn't a string array).
            log.error("Unexpected error while validating session token", e);
            return ResponseEntity.ok(SessionInfoResponse.invalid(GENERIC_INVALID_MESSAGE));
        }
    }

    /**
     * Best-effort classification of an expired-token condition from within a wrapped
     * {@link JwtException}. Needed because the default {@code JwtTimestampValidator}
     * rejects expired tokens during {@link JwtDecoder#decode(String)} itself, before the
     * post-decode expiry check can run, so the "expired" state must be recovered here to
     * satisfy the documented client contract.
     */
    private boolean isExpiredTokenException(JwtException e) {
        String message = e.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("expired");
    }

    /**
     * Session Information Response
     *
     * <p>Provides complete information about the current authentication session
     * without requiring authentication to access this data.
     */
    public record SessionInfoResponse(
        boolean authenticated,
        boolean expired,
        boolean valid,
        String userId,
        List<String> roles,
        Instant expiresAt,
        Long secondsRemaining,
        String error
    ) {
        public static SessionInfoResponse unauthenticated() {
            return new SessionInfoResponse(
                false, // not authenticated
                false, // not expired (no token)
                false, // no token means no valid session
                null,
                List.of(),
                null,
                null,
                null
            );
        }

        public static SessionInfoResponse expiredResponse() {
            return new SessionInfoResponse(
                false, // not authenticated (token expired)
                true,  // expired
                false, // not valid
                null,
                List.of(),
                null,
                null,  // secondsRemaining is null for expired tokens
                "Token expired"
            );
        }

        public static SessionInfoResponse invalid(String error) {
            return new SessionInfoResponse(
                false, // not authenticated
                false, // not expired (invalid token)
                false, // not valid
                null,
                List.of(),
                null,
                null,
                error
            );
        }

        public static SessionInfoResponse authenticatedResponse(
                String userId,
                List<String> roles,
                Instant expiresAt,
                long secondsRemaining) {
            return new SessionInfoResponse(
                true,  // authenticated
                false, // not expired
                true,  // valid
                userId,
                roles,
                expiresAt,
                secondsRemaining,
                null
            );
        }
    }
}
