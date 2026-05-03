package com.eshop.app.auth.aspect;

import com.eshop.app.common.constants.HttpHeaderNames;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

/**
 * Aspect for auditing authentication endpoint calls.
 * Logs security-relevant events and tracks metrics.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationAuditAspect {
    
    private final MeterRegistry meterRegistry;
    
    /**
     * Audits all authentication controller methods.
     */
    @Around("@within(org.springframework.web.bind.annotation.RestController) && " +
            "within(com.eshop.app.controller.OAuth2AuthController)")
    public Object auditAuthEndpoint(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return joinPoint.proceed();
        }
        
        String endpoint = extractEndpoint(joinPoint);
        String clientIp = getClientIp(request);
        String correlationId = MDC.get("correlationId");
        Instant startTime = Instant.now();
        
        log.debug("[{}] AUTH_REQUEST | endpoint={} | ip={} | method={}", 
                  correlationId, endpoint, clientIp, request.getMethod());
        
        try {
            Object result = joinPoint.proceed();
            
            // Log successful authentication events
            long duration = Duration.between(startTime, Instant.now()).toMillis();
            log.info("[{}] AUTH_SUCCESS | endpoint={} | ip={} | duration={}ms", 
                     correlationId, endpoint, clientIp, duration);
            
            meterRegistry.counter("auth.endpoint.success", 
                "endpoint", endpoint,
                "method", request.getMethod()).increment();
            
            meterRegistry.timer("auth.endpoint.duration", 
                "endpoint", endpoint).record(Duration.between(startTime, Instant.now()));
            
            return result;
            
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().is4xxClientError()) {
                log.warn("[{}] AUTH_CLIENT_ERROR | endpoint={} | ip={} | status={} | reason={}", 
                         correlationId, endpoint, clientIp, 
                         e.getStatusCode().value(), e.getReason());
                
                meterRegistry.counter("auth.endpoint.client_error", 
                    "endpoint", endpoint, 
                    "status", String.valueOf(e.getStatusCode().value())).increment();
            } else {
                log.error("[{}] AUTH_SERVER_ERROR | endpoint={} | ip={} | status={} | reason={}", 
                          correlationId, endpoint, clientIp, 
                          e.getStatusCode().value(), e.getReason());
                
                meterRegistry.counter("auth.endpoint.server_error", 
                    "endpoint", endpoint).increment();
            }
            throw e;
            
        } catch (Exception e) {
            log.error("[{}] AUTH_EXCEPTION | endpoint={} | ip={} | error={}", 
                      correlationId, endpoint, clientIp, e.getMessage());
            
            meterRegistry.counter("auth.endpoint.exception", 
                "endpoint", endpoint,
                "exception", e.getClass().getSimpleName()).increment();
            
            throw e;
        }
    }
    
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
    
    private String extractEndpoint(ProceedingJoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        return methodName;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader(HttpHeaderNames.X_FORWARDED_FOR);
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
