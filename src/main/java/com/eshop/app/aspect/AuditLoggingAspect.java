package com.eshop.app.aspect;

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
 * Aspect for audit logging
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLoggingAspect {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.eshop.app.aspect.Auditable)")
    public Object logAudit(ProceedingJoinPoint joinPoint) throws Throwable {
        // Note: we declare the annotation param via reflection below to avoid compile-time coupling in case of proxies
        Object result = null;
        boolean success = true;
        String errorMessage = null;

        Auditable auditable = null;
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            auditable = signature.getMethod().getAnnotation(Auditable.class);
        } catch (Exception ignored) {
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
                log.error("Failed to log audit", e);
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
            log.error("Failed to save audit log", e);
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
        String username = SecurityUtils.getCurrentUsername().orElse("system");

        HttpServletRequest request = getCurrentHttpRequest();
        String ipAddress = request != null ? getClientIp(request) : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        AuditLog auditLog = AuditLog.builder()
                .action(auditable.action())
                .entityType(auditable.entityType().isEmpty() ? className : auditable.entityType())
                .description(auditable.description().isEmpty() ? methodName : auditable.description())
                .userIdentifier(userId)
                .username(username)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(success)
                .timestamp(LocalDateTime.now())
                .build();

        if (auditable.logArgs() && joinPoint.getArgs() != null && joinPoint.getArgs().length > 0) {
            try {
                String argsJson = objectMapper.writeValueAsString(joinPoint.getArgs());
                auditLog.setOldValue(argsJson);
            } catch (Exception e) {
                log.warn("Failed to serialize method arguments", e);
            }
        }

        if (auditable.logResult() && result != null) {
            try {
                String resultJson = objectMapper.writeValueAsString(result);
                auditLog.setNewValue(resultJson);
            } catch (Exception e) {
                log.warn("Failed to serialize method result", e);
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
                Object firstArg = args[0];
                if (firstArg instanceof Long) {
                    auditLog.setEntityId(String.valueOf(firstArg));
                } else if (firstArg instanceof String) {
                    auditLog.setEntityId((String) firstArg);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract entity id: {}", e.getMessage());
        }
    }

    private HttpServletRequest getCurrentHttpRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
