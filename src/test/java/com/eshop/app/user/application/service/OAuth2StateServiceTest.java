package com.eshop.app.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.eshop.app.user.domain.exception.OAuth2CsrfException;
import com.eshop.app.user.domain.exception.OAuth2StateException;
import com.eshop.app.user.infrastructure.config.OAuth2StateConfig;
import com.eshop.app.user.infrastructure.util.NonceGenerator;
import com.eshop.app.user.infrastructure.util.SystemClock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAuth2StateServiceTest {

    @Mock private OAuth2StateConfig config;
    @Mock private SystemClock clock;
    @Mock private NonceGenerator nonceGenerator;

    @InjectMocks private OAuth2StateService stateService;

    private static final String SIGNING_KEY =
            "dedicated-oauth2-state-signing-secret-key-minimum-256-bits";
    private static final String REDIRECT_URI = "http://localhost:3000/callback";

    @BeforeEach
    void setUp() {
        lenient().when(config.getSigningKey()).thenReturn(SIGNING_KEY);
        lenient().when(config.getValidity()).thenReturn(Duration.ofMinutes(5));
        lenient().when(config.getClockSkewTolerance()).thenReturn(Duration.ofMinutes(1));
    }

    @Test
    void generateState_shouldCreateValidStateToken() {
        // Given
        Instant now = Instant.parse("2026-05-31T10:00:00Z");
        when(clock.now()).thenReturn(now);
        when(nonceGenerator.generate()).thenReturn("test-nonce");

        // When
        String state = stateService.generateState(REDIRECT_URI).block();

        // Then
        assertThat(state).isNotBlank();

        // When validating
        stateService.validateState(state, REDIRECT_URI).block(); // Should pass without throwing
    }

    @Test
    void validateState_shouldThrowWhenStateExpired() {
        // Given
        Instant now = Instant.parse("2026-05-31T10:00:00Z");
        when(clock.now()).thenReturn(now);
        when(nonceGenerator.generate()).thenReturn("test-nonce");

        String state = stateService.generateState(REDIRECT_URI).block();

        // State is validated 6 minutes later
        when(clock.now()).thenReturn(now.plus(Duration.ofMinutes(6)));

        // Then
        assertThatThrownBy(() -> stateService.validateState(state, REDIRECT_URI).block())
                .isInstanceOf(OAuth2StateException.class)
                .hasMessageContaining("State expired");
    }

    @Test
    void validateState_shouldThrowWhenRedirectUriMismatch() {
        // Given
        Instant now = Instant.parse("2026-05-31T10:00:00Z");
        when(clock.now()).thenReturn(now);
        when(nonceGenerator.generate()).thenReturn("test-nonce");

        String state = stateService.generateState(REDIRECT_URI).block();

        // Then
        assertThatThrownBy(() -> stateService.validateState(state, "http://malicious.com").block())
                .isInstanceOf(OAuth2CsrfException.class)
                .hasMessageContaining("Redirect URI mismatch");
    }

    @Test
    void validateState_shouldThrowWhenSignatureInvalid() {
        // Given
        Instant now = Instant.parse("2026-05-31T10:00:00Z");
        when(clock.now()).thenReturn(now);
        when(nonceGenerator.generate()).thenReturn("test-nonce");

        String state = stateService.generateState(REDIRECT_URI).block();

        // Change signing key on config
        when(config.getSigningKey()).thenReturn("another-secret-signing-key-minimum-256-bits");

        // Then
        assertThatThrownBy(() -> stateService.validateState(state, REDIRECT_URI).block())
                .isInstanceOf(OAuth2CsrfException.class)
                .hasMessageContaining("Invalid signature");
    }
}
