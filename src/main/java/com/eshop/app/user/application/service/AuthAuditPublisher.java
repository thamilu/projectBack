package com.eshop.app.user.application.service;

import com.eshop.app.user.domain.event.AuthAuditEvent;
import com.eshop.app.user.domain.event.AuthEventType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthAuditPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishLoginSuccess(
            String email, String userId, String ipAddress, String userAgent, long durationMs) {
        AuthAuditEvent event =
                AuthAuditEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType(AuthEventType.LOGIN_SUCCESS)
                        .timestamp(Instant.now())
                        .userId(userId)
                        .email(email)
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .action("LOGIN")
                        .success(true)
                        .metadata(Map.of("durationMs", String.valueOf(durationMs)))
                        .build();
        eventPublisher.publishEvent(event);
    }

    public void publishLoginFailure(
            String email, String ipAddress, String userAgent, String reason) {
        AuthAuditEvent event =
                AuthAuditEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType(AuthEventType.LOGIN_FAILURE)
                        .timestamp(Instant.now())
                        .email(email)
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .action("LOGIN")
                        .success(false)
                        .failureReason(reason)
                        .build();
        eventPublisher.publishEvent(event);
    }

    public void publishCsrfAttempt(String ipAddress, String userAgent, String state) {
        AuthAuditEvent event =
                AuthAuditEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType(AuthEventType.OAUTH2_CSRF_ATTEMPT)
                        .timestamp(Instant.now())
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .action("OAUTH2_CALLBACK")
                        .success(false)
                        .failureReason("CSRF_VALIDATION_FAILED")
                        .metadata(state != null ? Map.of("state", maskState(state)) : Map.of())
                        .build();
        eventPublisher.publishEvent(event);
    }

    public void publishTokenRefreshSuccess(
            String email, String userId, String ipAddress, String userAgent) {
        AuthAuditEvent event =
                AuthAuditEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType(AuthEventType.TOKEN_REFRESH_SUCCESS)
                        .timestamp(Instant.now())
                        .userId(userId)
                        .email(email)
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .action("TOKEN_REFRESH")
                        .success(true)
                        .build();
        eventPublisher.publishEvent(event);
    }

    public void publishTokenRefreshFailure(String ipAddress, String userAgent, String reason) {
        AuthAuditEvent event =
                AuthAuditEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType(AuthEventType.TOKEN_REFRESH_FAILURE)
                        .timestamp(Instant.now())
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .action("TOKEN_REFRESH")
                        .success(false)
                        .failureReason(reason)
                        .build();
        eventPublisher.publishEvent(event);
    }

    public void publishLogout(String email, String userId, String ipAddress, String userAgent) {
        AuthAuditEvent event =
                AuthAuditEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType(AuthEventType.LOGOUT)
                        .timestamp(Instant.now())
                        .userId(userId)
                        .email(email)
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .action("LOGOUT")
                        .success(true)
                        .build();
        eventPublisher.publishEvent(event);
    }

    public void publishTokenIntrospection(
            String userId, String ipAddress, String userAgent, boolean active) {
        AuthAuditEvent event =
                AuthAuditEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType(AuthEventType.TOKEN_INTROSPECTION)
                        .timestamp(Instant.now())
                        .userId(userId)
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .action("TOKEN_INTROSPECT")
                        .success(active)
                        .build();
        eventPublisher.publishEvent(event);
    }

    private String maskState(String state) {
        if (state == null || state.length() < 8) return "***";
        return state.substring(0, 4) + "..." + state.substring(state.length() - 4);
    }
}
