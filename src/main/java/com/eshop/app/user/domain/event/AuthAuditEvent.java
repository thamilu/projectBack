package com.eshop.app.user.domain.event;

import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthAuditEvent {
    String eventId; // UUID
    AuthEventType eventType; // LOGIN_SUCCESS, LOGIN_FAILURE, etc.
    Instant timestamp;
    String userId; // null for failed logins
    String email;
    String ipAddress;
    String userAgent;
    String action; // "LOGIN", "LOGOUT", "TOKEN_REFRESH", etc.
    boolean success;
    String failureReason;
    Map<String, String> metadata;
}
