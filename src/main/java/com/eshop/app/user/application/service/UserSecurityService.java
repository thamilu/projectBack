package com.eshop.app.user.application.service;

import com.eshop.app.core.exception.base.BusinessException;
import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import com.eshop.app.core.infrastructure.config.security.oauth.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Service-layer authorization guard used by {@code UserCommandService} (the
 * {@code ManageUserUseCase} implementation) as a defense-in-depth check independent of
 * the {@code @PreAuthorize}/SpEL checks already enforced at the controller layer (backed
 * by {@code UserSecurityExpression}, the {@code @userSecurity} bean — a distinct class
 * from this one; they serve different layers and are not interchangeable).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSecurityService {

    private final AppProperties appProperties;

    public void checkCanModifyUser(Long targetUserId, Authentication auth) {
        if (hasAdminRole(auth)) {
            return; // Admins can modify any user
        }

        Long currentUserId = extractUserIdOrNull(auth);
        // Objects.equals instead of currentUserId.equals(...): PrincipalDetails.getId()
        // can legitimately be null for an unresolved local identity (see
        // UserController's JIT-sync fallback for GET/PUT /users/me), and calling
        // .equals() directly on that null previously threw an unhandled
        // NullPointerException (500) instead of a clean, expected 403 for exactly the
        // scenario the rest of this codebase treats as normal and recoverable.
        if (!Objects.equals(currentUserId, targetUserId)) {
            log.warn(
                    "User {} attempted to modify user {} without permission",
                    currentUserId,
                    targetUserId);
            throw new AccessDeniedException("You don't have permission to modify this user");
        }
    }

    public void checkCanChangeRole(Authentication auth) {
        if (!hasAdminRole(auth)) {
            log.warn("Non-admin user attempted to change role");
            throw new AccessDeniedException("Only administrators can change user roles");
        }
    }

    public void checkCanPerformBulkOperation(int userCount, Authentication auth) {
        if (!hasAdminRole(auth)) {
            throw new AccessDeniedException("Only administrators can perform bulk operations");
        }

        int maxBulkSize = appProperties.getSecurity().getBulkOperationMaxSize();
        if (userCount > maxBulkSize) {
            throw new BusinessException(
                    "Bulk operations limited to " + maxBulkSize + " users",
                    "BULK_LIMIT_EXCEEDED",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private boolean hasAdminRole(Authentication auth) {
        return hasRole(auth, appProperties.getSecurity().getRoles().getAdmin());
    }

    private boolean hasRole(Authentication auth, String role) {
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        String expectedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(expectedRole));
    }

    /**
     * Extracts the local user ID from the given Authentication.
     *
     * @throws AccessDeniedException if the principal isn't a recognized
     *         {@link PrincipalDetails} (this method is only ever reached from an
     *         already-authenticated request, so this is a defensive/unreachable-in-
     *         practice branch — {@link AccessDeniedException} maps to a clean 403 here
     *         rather than the unmapped 500 an {@code IllegalStateException} would produce)
     */
    public Long extractUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof PrincipalDetails principalDetails) {
            return principalDetails.getId();
        }
        throw new AccessDeniedException("Unable to extract user identity from authentication context");
    }

    /**
     * Null-tolerant variant used internally where an unresolved identity must result in
     * a deny decision via {@link Objects#equals}, not an exception.
     */
    private Long extractUserIdOrNull(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof PrincipalDetails principalDetails) {
            return principalDetails.getId();
        }
        return null;
    }
}
