package com.eshop.app.user.application.service;

import com.eshop.app.inventory.application.service.UserAuditService;
import com.eshop.app.user.shared.domain.enums.UserAction;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Decorates UserAuditService with exception isolation. Prevents log storage failures from aborting
 * the primary transaction flow.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SafeAuditService {

    private final UserAuditService delegate;

    /**
     * Logs user actions safely.
     *
     * @param actorId The actor performing the action
     * @param targetId The target of the action
     * @param action The user action enum
     */
    public void logUserAction(Long actorId, Long targetId, UserAction action) {
        try {
            delegate.logUserAction(actorId, targetId, action);
        } catch (Exception e) {
            log.error(
                    "AUDIT_FAILURE: Failed to log action={} actor={} target={}: {}",
                    action,
                    actorId,
                    targetId,
                    e.getMessage());
        }
    }

    /**
     * Logs bulk actions safely.
     *
     * @param actorId The actor performing the action
     * @param targetIds The list of targets of the bulk action
     * @param action The user action enum
     */
    public void logBulkAction(Long actorId, List<Long> targetIds, UserAction action) {
        try {
            delegate.logBulkAction(actorId, targetIds, action);
        } catch (Exception e) {
            log.error(
                    "AUDIT_FAILURE: Failed to log bulk action={} actor={}: {}",
                    action,
                    actorId,
                    e.getMessage());
        }
    }
}
