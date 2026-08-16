package com.eshop.app.user.application.service.impl;

import com.eshop.app.inventory.domain.entity.AuditLog;
import com.eshop.app.inventory.domain.repository.AuditLogRepository;
import com.eshop.app.inventory.shared.domain.enums.AuditAction;
import com.eshop.app.user.domain.event.AuthAuditEvent;
import com.eshop.app.user.domain.event.AuthEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asynchronous Spring event listener that persists {@link AuthAuditEvent} instances as
 * {@link AuditLog} records for security audit and compliance purposes.
 *
 * <p><b>Async execution:</b> Processed on the {@code "auditExecutor"} thread pool to avoid
 * blocking the auth flow that publishes the event. Failures are caught and logged — they do NOT
 * propagate back to the publisher. A dropped audit record is logged as an error, but the
 * originating authentication operation is not affected.
 *
 * <p><b>Failure behavior:</b> If persistence fails, the failure is logged at ERROR level with the
 * {@code eventId} for manual recovery. No retry or dead-letter mechanism is currently implemented
 * — see architecture backlog for compensating action design.
 *
 * <p><b>Architecture note:</b> This class depends directly on {@code inventory} bounded context
 * domain types ({@link AuditLog}, {@link AuditLogRepository}, {@link AuditAction}). This is a
 * cross-module domain dependency that violates bounded context isolation and must be resolved by
 * relocating audit infrastructure to a shared module or introducing an anti-corruption port.
 * Tracked as architecture debt — do not introduce further cross-module domain imports.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthAuditEventListener {

    // Maximum length for attacker-controlled header fields stored in the audit log.
    // Prevents log injection and oversized payload attacks via crafted User-Agent / IP headers.
    private static final int MAX_HEADER_FIELD_LENGTH = 512;

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Handles an {@link AuthAuditEvent} by mapping it to an {@link AuditLog} entity and
     * persisting it. Executed asynchronously on the {@code "auditExecutor"} thread pool.
     *
     * @param event the auth audit event published by the security layer; must not be null
     */
    @Async("auditExecutor")
    @EventListener
    @Transactional
    public void handleAuthAuditEvent(AuthAuditEvent event) {
        if (event == null) {
            log.error("Received null AuthAuditEvent — event discarded");
            return;
        }

        log.debug(
                "Received AuthAuditEvent: eventId=[{}] eventType=[{}]",
                event.getEventId(),
                event.getEventType());

        try {
            if (event.getEventType() == null) {
                log.error(
                        "AuthAuditEvent has null eventType — event discarded: eventId=[{}]",
                        event.getEventId());
                return;
            }

            AuditAction auditAction = mapToAuditAction(event.getEventType());

            LocalDateTime utcTimestamp =
                    LocalDateTime.ofInstant(event.getTimestamp(), ZoneOffset.UTC);

            AuditLog auditLog =
                    AuditLog.builder()
                            .userIdentifier(event.getUserId())
                            .email(event.getEmail())
                            .action(auditAction)
                            .description(buildDescription(event))
                            .ipAddress(sanitizeHeader(event.getIpAddress()))
                            .userAgent(sanitizeHeader(event.getUserAgent()))
                            .success(event.isSuccess())
                            .errorMessage(event.getFailureReason())
                            .timestamp(utcTimestamp)
                            .metadata(serializeMetadata(event))
                            .build();

            auditLogRepository.save(auditLog);

            log.info(
                    "Auth audit log persisted: eventId=[{}] action=[{}] success=[{}]",
                    event.getEventId(),
                    auditAction,
                    event.isSuccess());

        } catch (Exception e) {
            log.error(
                    "Failed to persist auth audit event: eventId=[{}]",
                    event.getEventId(),
                    e);
        }
    }

    // -------------------------------------------------------------------------
    // Private — Mapping
    // -------------------------------------------------------------------------

    private AuditAction mapToAuditAction(AuthEventType eventType) {
        return switch (eventType) {
            case LOGIN_SUCCESS, OAUTH2_CALLBACK_SUCCESS -> AuditAction.USER_LOGIN;
            case LOGIN_FAILURE                          -> AuditAction.USER_LOGIN_FAILED;
            case LOGOUT                                 -> AuditAction.USER_LOGOUT;
            case TOKEN_REFRESH_SUCCESS                  -> AuditAction.USER_TOKEN_REFRESH;
            case TOKEN_REFRESH_FAILURE                  -> AuditAction.USER_TOKEN_REFRESH_FAILED;
            case OAUTH2_CSRF_ATTEMPT                    -> AuditAction.OAUTH2_CSRF_ATTEMPT;
            case TOKEN_INTROSPECTION                    -> AuditAction.TOKEN_INTROSPECT;
        };
    }

    private String buildDescription(AuthAuditEvent event) {
        return String.format("%s - %s", event.getEventType().name(), event.getAction());
    }

    // -------------------------------------------------------------------------
    // Private — Metadata Serialization
    // -------------------------------------------------------------------------

    /**
     * Serializes event metadata to a JSON string for structured storage in the audit log.
     * Falls back to {@code null} if metadata is absent or serialization fails.
     *
     * @param event the source audit event
     * @return JSON string representation of metadata, or {@code null} if none/unserializable
     */
    private String serializeMetadata(AuthAuditEvent event) {
        if (event.getMetadata() == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(event.getMetadata());
        } catch (JsonProcessingException e) {
            log.warn(
                    "Could not serialize metadata for audit event: eventId=[{}] reason=[{}]",
                    event.getEventId(),
                    e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Private — Input Sanitization
    // -------------------------------------------------------------------------

    /**
     * Sanitizes an attacker-controlled HTTP header value (e.g., {@code X-Forwarded-For},
     * {@code User-Agent}) before persisting it to the audit log.
     *
     * <p>Removes ASCII control characters (0x00–0x1F, 0x7F) to prevent log injection and SIEM
     * poisoning, and truncates to {@link #MAX_HEADER_FIELD_LENGTH} characters to prevent
     * oversized payload attacks.
     *
     * @param headerValue the raw header value from the request; may be null
     * @return the sanitized value, or {@code null} if the input was null
     */
    private String sanitizeHeader(String headerValue) {
        if (headerValue == null) {
            return null;
        }
        // Remove ASCII control characters to prevent log/SIEM injection
        String sanitized = headerValue.replaceAll("[\\x00-\\x1F\\x7F]", "");

        // Truncate to maximum safe length
        if (sanitized.length() > MAX_HEADER_FIELD_LENGTH) {
            sanitized = sanitized.substring(0, MAX_HEADER_FIELD_LENGTH);
        }
        return sanitized;
    }
}

