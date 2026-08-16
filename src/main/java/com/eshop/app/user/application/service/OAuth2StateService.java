package com.eshop.app.user.application.service;

import com.eshop.app.user.domain.exception.OAuth2CsrfException;
import com.eshop.app.user.domain.exception.OAuth2StateException;
import com.eshop.app.user.infrastructure.config.OAuth2StateConfig;
import com.eshop.app.user.infrastructure.util.HmacUtil;
import com.eshop.app.user.infrastructure.util.NonceGenerator;
import com.eshop.app.user.infrastructure.util.SystemClock;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2StateService {

    private final OAuth2StateConfig config;
    private final SystemClock clock;
    private final NonceGenerator nonceGenerator;

    private final Scheduler cryptoScheduler =
            Schedulers.newBoundedElastic(10, 1000, "crypto-ops", 60, true);

    public Mono<String> generateState(String redirectUri) {
        return Mono.fromCallable(() -> generateStateSync(redirectUri)).subscribeOn(cryptoScheduler);
    }

    private String generateStateSync(String redirectUri) {
        String nonce = nonceGenerator.generate();
        long timestamp = clock.now().toEpochMilli();
        String payload = buildPayload(nonce, redirectUri, timestamp);
        String signature = HmacUtil.hmacSha256(payload, config.getSigningKey());
        String stateStr = payload + "|" + signature;
        return Base64.getUrlEncoder().encodeToString(stateStr.getBytes(StandardCharsets.UTF_8));
    }

    public Mono<Void> validateState(String state, String redirectUri) {
        return Mono.<Void>fromRunnable(() -> validateStateSync(state, redirectUri))
                .subscribeOn(cryptoScheduler);
    }

    private void validateStateSync(String state, String redirectUri) {
        if (state == null || state.isBlank()) {
            throw new OAuth2StateException("State parameter is required");
        }

        StateToken token = parseState(state);
        validateTimestamp(token.timestamp());
        validateRedirectUri(token.redirectUri(), redirectUri);
        validateSignature(token);
    }

    public void validateStateFormat(String state) {
        if (state == null || state.isBlank()) {
            throw new OAuth2StateException("State cannot be null or blank");
        }
        parseState(state);
    }

    public StateToken parseState(String state) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(state);
            String decodedStr = new String(decoded, StandardCharsets.UTF_8);
            String[] parts = decodedStr.split("\\|");

            if (parts.length != 4) {
                throw new OAuth2StateException("Malformed state parameter");
            }

            return new StateToken(
                    parts[0], // nonce
                    parts[1], // redirectUri
                    Long.parseLong(parts[2]), // timestamp
                    parts[3] // signature
                    );
        } catch (IllegalArgumentException e) {
            throw new OAuth2StateException("Invalid state encoding", e);
        } catch (Exception e) {
            throw new OAuth2StateException("Failed to parse state", e);
        }
    }

    private void validateTimestamp(long timestamp) {
        long now = clock.now().toEpochMilli();
        long age = now - timestamp;

        if (age > config.getValidity().toMillis()) {
            throw new OAuth2StateException("State expired");
        }
        if (age < -config.getClockSkewTolerance().toMillis()) {
            throw new OAuth2StateException("State timestamp in future (clock skew)");
        }
    }

    private void validateRedirectUri(String expected, String actual) {
        String normalizedExpected = Optional.ofNullable(expected).orElse("");
        String normalizedActual = Optional.ofNullable(actual).orElse("");

        // Constant-time comparison to prevent timing attacks
        if (!MessageDigest.isEqual(
                normalizedExpected.getBytes(StandardCharsets.UTF_8),
                normalizedActual.getBytes(StandardCharsets.UTF_8))) {
            log.warn(
                    "OAuth2 CSRF: redirect URI mismatch. Expected=[{}] Got=[{}]", expected, actual);
            throw new OAuth2CsrfException("CSRF validation failed: Redirect URI mismatch");
        }
    }

    private void validateSignature(StateToken token) {
        String payload = buildPayload(token.nonce(), token.redirectUri(), token.timestamp());
        String expected = HmacUtil.hmacSha256(payload, config.getSigningKey());

        // Constant-time comparison to prevent timing attacks
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                token.signature().getBytes(StandardCharsets.UTF_8))) {
            log.error("OAuth2 CSRF ATTACK DETECTED: Invalid state signature");
            throw new OAuth2CsrfException("CSRF validation failed: Invalid signature");
        }
    }

    private String buildPayload(String nonce, String redirectUri, long timestamp) {
        return String.format(
                "%s|%s|%d", nonce, Optional.ofNullable(redirectUri).orElse(""), timestamp);
    }

    public record StateToken(String nonce, String redirectUri, long timestamp, String signature) {}
}
