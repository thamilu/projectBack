package com.eshop.app.core.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * AOP Aspect for automatic audit trail generation.
 *
 * <p>
 * Intercepts any method annotated with {@link Auditable} and dispatches audit-record
 * construction and persistence to {@link AuditLogPersister}, which runs it asynchronously
 * (who, what, when, source IP, success/failure).
 *
 * <p>
 * Part of the Enterprise Core Audit Layer. All sensitive administrative,
 * financial, and seller operations MUST use {@link Auditable} to maintain
 * a tamper-evident compliance trail.
 *
 * @see Auditable
 * @since 2.0
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLoggingAspect {

    private final AuditLogPersister auditLogPersister;

    @Around("@annotation(com.eshop.app.core.audit.Auditable)")
    public Object logAudit(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = null;
        boolean success = true;
        String errorMessage = null;
        Auditable auditable = null;

        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            auditable = signature.getMethod().getAnnotation(Auditable.class);
        } catch (Exception ignored) {
            // Non-fatal: proceed without audit metadata
        }

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            success = false;
            errorMessage = throwable.getMessage();
            log.error("Error during audited method execution", throwable);
            throw throwable;
        } finally {
            try {
                auditLogPersister.persistAsync(joinPoint, auditable, result, success, errorMessage);
            } catch (Exception e) {
                log.error("Failed to dispatch async audit log", e);
            }
        }
    }
}
