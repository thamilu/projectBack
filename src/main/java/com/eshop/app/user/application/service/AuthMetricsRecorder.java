package com.eshop.app.user.application.service;

import com.eshop.app.core.observability.MetricsService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthMetricsRecorder {

    private final MetricsService metricsService;

    public void recordLoginAttempt(String provider, boolean success) {
        metricsService.incrementCounter(
                "auth.login.attempts",
                "provider",
                provider,
                "status",
                success ? "success" : "failure");
    }

    public void recordLoginDuration(String provider, Duration duration, boolean success) {
        metricsService.recordTimer(
                "auth.login.duration",
                duration.toMillis(),
                "provider",
                provider,
                "status",
                success ? "success" : "failure");
    }

    public void recordTokenRefresh(boolean success) {
        metricsService.incrementCounter(
                "auth.token.refresh", "status", success ? "success" : "failure");
    }

    public void recordOAuth2StateValidation(boolean success, String failureReason) {
        metricsService.incrementCounter(
                "auth.oauth2.state.validation",
                "status",
                success ? "success" : "failure",
                "reason",
                failureReason != null ? failureReason : "none");
    }

    public void recordCsrfAttempt(String ipAddress) {
        metricsService.incrementCounter("auth.security.csrf.attempts", "ip", hashIp(ipAddress));
    }

    public void recordMalformedStateAttempt(String ipAddress) {
        metricsService.incrementCounter(
                "auth.security.malformed_state.attempts", "ip", hashIp(ipAddress));
    }

    private String hashIp(String ip) {
        if (ip == null) {
            return "unknown";
        }
        // Simple hash to obscure IP in metrics while preserving cardinality groupings
        return String.valueOf(ip.hashCode() & 0xFFFF);
    }
}
