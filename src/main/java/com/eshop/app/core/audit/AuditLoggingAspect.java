package com.eshop.app.core.audit;

import com.eshop.app.entity.AuditLog;
import com.eshop.app.repository.AuditLogRepository;
import com.eshop.app.util.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * AOP Aspect for automatic audit trail generation.
 *
 * <p>Intercepts any method annotated with {@link Auditable} and asynchronously
 * persists a complete audit record (who, what, when, source IP, success/failure).
 *
 * <p>Part of the Enterprise Core Audit Layer. All sensitive administrative,
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

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

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
                logAuditAsync(joinPoint, auditable, result, success, errorMessage);
            } catch (Exception e) {
                log.error("Failed to dispatch async audit log", e);
            }
        }
    }

    @Async
    protected void logAuditAsync(
            ProceedingJoinPoint joinPoint,
            Auditable auditable,
            Object result,
            boolean success,
            String errorMessage) {

        if (auditable == null) return;

        try {
            AuditLog auditLog = buildAuditLog(joinPoint, auditable, result, success, errorMessage);
            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: {}", auditLog.getId());
        } catch (Exception e) {
            log.error("Failed to persist audit log", e);
        }
    }

    private AuditLog buildAuditLog(
            ProceedingJoinPoint joinPoint,
            Auditable auditable,
            Object result,
            boolean success,
            String errorMessage) {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();
        String className = signature.getDeclaringType().getSimpleName();

        String userId = SecurityUtils.getCurrentUserId().orElse("system");
        String email = SecurityUtils.getCurrentEmail().orElse("system");

        HttpServletRequest request = getCurrentHttpRequest();
        String ipAddress = request != null ? resolveClientIp(request) : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        AuditLog auditLog = AuditLog.builder()
                .action(auditable.action())
                .entityType(auditable.entityType().isEmpty() ? className : auditable.entityType())
                .description(auditable.description().isEmpty() ? methodName : auditable.description())
                .userIdentifier(userId)
                .email(email)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(success)
                .timestamp(LocalDateTime.now())
                .build();

        if (auditable.logArgs() && joinPoint.getArgs() != null && joinPoint.getArgs().length > 0) {
            try {
                auditLog.setOldValue(objectMapper.writeValueAsString(joinPoint.getArgs()));
            } catch (Exception e) {
                log.warn("Failed to serialize method arguments for audit: {}", e.getMessage());
            }
        }

        if (auditable.logResult() && result != null) {
            try {
                auditLog.setNewValue(objectMapper.writeValueAsString(result));
            } catch (Exception e) {
                log.warn("Failed to serialize method result for audit: {}", e.getMessage());
            }
        }

        if (!success && errorMessage != null) {
            auditLog.setErrorMessage(errorMessage);
        }

        extractEntityId(joinPoint, auditLog);
        return auditLog;
    }

    private void extractEntityId(ProceedingJoinPoint joinPoint, AuditLog auditLog) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                Object first = args[0];
                if (first instanceof Long) {
                    auditLog.setEntityId(String.valueOf(first));
                } else if (first instanceof String) {
                    auditLog.setEntityId((String) first);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract entity ID for audit: {}", e.getMessage());
        }
    }

    private HttpServletRequest getCurrentHttpRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Resolves real client IP, respecting reverse-proxy headers (X-Forwarded-For, X-Real-IP).
     */
    private String resolveClientIp(HttpServletRequest request) {
        for (String header : new String[]{"X-Forwarded-For", "X-Real-IP"}) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.contains(",") ? ip.split(",")[0].trim() : ip;
            }
        }
        return request.getRemoteAddr();
    }
}
