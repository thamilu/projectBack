package com.eshop.app.user.infrastructure.security;

import com.eshop.app.core.exception.base.BusinessException;

import io.micrometer.core.instrument.MeterRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;

/**
 * Enforces business logic safety checks preventing admins from mutating their own status/roles
 * (e.g. self-deactivation, self-role-demotion) — operations that could cause irreversible
 * privilege lockout if performed on oneself.
 *
 * <p>Null semantics: a null {@code targetId} or {@code currentUserId} cannot be "self", so the
 * guard passes silently rather than throwing — callers remain responsible for validating that
 * these fields are present where required. {@code operation} must never be null; a null value
 * indicates a programming error and fails fast via {@link Objects#requireNonNull}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserSelfProtectionGuard {

    private final MeterRegistry meterRegistry;

    /**
     * Prevents a user from performing a protected operation on themselves.
     *
     * @param targetId      the ID of the entity being mutated; a null value cannot match
     *                      {@code currentUserId} so the guard passes
     * @param currentUserId the currently authenticated user's ID; a null value means no
     *                      authenticated user to compare against, so the guard passes
     * @param operation     the protected operation being attempted; must not be null
     * @throws BusinessException with {@code 403 Forbidden} if {@code targetId} equals
     *                           {@code currentUserId}
     */
    public void preventSelfOperation(
            Long targetId, Long currentUserId, SelfProtectedOperation operation) {
        Objects.requireNonNull(operation, "SelfProtectedOperation must not be null");

        if (targetId == null || currentUserId == null) {
            return;
        }
        if (targetId.equals(currentUserId)) {
            recordAndThrow(currentUserId, operation, false);
        }
    }

    /**
     * Prevents a user from including themselves in a bulk protected operation.
     *
     * @param targetIds     the collection of target IDs being mutated in bulk; a null or empty
     *                      collection means nothing to check, so the guard passes
     * @param currentUserId the currently authenticated user's ID; a null value means no
     *                      authenticated user to compare against, so the guard passes
     * @param operation     the protected operation being attempted; must not be null
     * @throws BusinessException with {@code 400 Bad Request} if {@code targetIds} contains a
     *                           null element (malformed input, not a policy violation)
     * @throws BusinessException with {@code 403 Forbidden} if {@code targetIds} contains
     *                           {@code currentUserId}
     */
    public void preventSelfOperationInBulk(
            Collection<Long> targetIds, Long currentUserId, SelfProtectedOperation operation) {
        Objects.requireNonNull(operation, "SelfProtectedOperation must not be null");

        if (targetIds == null || targetIds.isEmpty() || currentUserId == null) {
            return;
        }
        if (targetIds.stream().anyMatch(Objects::isNull)) {
            log.warn("[GUARD] Bulk {} rejected: target ID collection contains a null element",
                    operation.name().toLowerCase());
            throw new BusinessException(
                    "Target ID collection must not contain null elements",
                    "INVALID_TARGET_IDS",
                    HttpStatus.BAD_REQUEST);
        }
        if (targetIds.contains(currentUserId)) {
            recordAndThrow(currentUserId, operation, true);
        }
    }

    private void recordAndThrow(Long currentUserId, SelfProtectedOperation operation, boolean bulk) {
        log.warn("[SECURITY][GUARD] User {} attempted {}self-{} — blocked",
                currentUserId, bulk ? "bulk " : "", operation.name().toLowerCase());

        meterRegistry.counter("security.self_protection.violation",
                        "operation", operation.name().toLowerCase(),
                        "scope", bulk ? "bulk" : "single")
                .increment();

        throw new BusinessException(
                operation.getMessage(), operation.getErrorCode(), HttpStatus.FORBIDDEN);
    }
}
