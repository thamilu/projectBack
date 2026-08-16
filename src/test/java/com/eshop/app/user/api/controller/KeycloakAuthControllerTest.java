package com.eshop.app.user.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.eshop.app.core.exception.infrastructure.KeycloakException;
import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import com.eshop.app.user.api.request.LoginRequest;
import com.eshop.app.user.api.request.RefreshTokenRequest;
import com.eshop.app.user.api.response.TokenResponse;
import com.eshop.app.user.application.service.KeycloakAuthService;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Verifies that login/refresh/callback/userinfo let errors propagate to
 * {@code AuthControllerExceptionHandler} instead of being swallowed into an
 * empty-body status response, and covers the redirect-URI allow-list validation
 * and Bearer-token parsing added to close the open-redirect and malformed-header
 * gaps identified in review.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KeycloakAuthControllerTest {

    @Mock KeycloakAuthService authService;

    private AppProperties appProperties;

    @InjectMocks KeycloakAuthController controller;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getSecurity().setDefaultRedirectUri("https://app.example.com/default-callback");
        appProperties.getSecurity().setAllowedRedirectUris(List.of("https://app.example.com"));
        controller = new KeycloakAuthController(authService, appProperties);
    }

    @Test
    void login_propagatesKeycloakExceptionInsteadOfSwallowingIt() {
        LoginRequest request = LoginRequest.builder().email("user@example.com").password("wrong").build();
        when(authService.login(request))
                .thenReturn(Mono.error(new KeycloakException(
                        "Authentication failed: Invalid email or password", HttpStatus.UNAUTHORIZED)));

        KeycloakException ex = assertThrows(
                KeycloakException.class, () -> controller.login(request).block());

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void login_propagatesUnexpectedErrorsRatherThanMisreportingAsUnauthorized() {
        LoginRequest request = LoginRequest.builder().email("user@example.com").password("x").build();
        when(authService.login(request)).thenReturn(Mono.error(new TimeoutException("connect timed out")));

        assertThrows(RuntimeException.class, () -> controller.login(request).block());
    }

    @Test
    void refreshToken_propagatesKeycloakException() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("expired-token");
        when(authService.refreshToken(request))
                .thenReturn(Mono.error(new KeycloakException("Token refresh failed", HttpStatus.UNAUTHORIZED)));

        assertThrows(KeycloakException.class, () -> controller.refreshToken(request).block());
    }

    @Test
    void handleCallback_propagatesKeycloakException() {
        when(authService.exchangeAuthorizationCode("bad-code", "https://app.example.com/callback"))
                .thenReturn(Mono.error(
                        new KeycloakException("Authorization code exchange failed", HttpStatus.BAD_REQUEST)));

        KeycloakException ex = assertThrows(
                KeycloakException.class,
                () -> controller.handleCallback("bad-code", null, "https://app.example.com/callback").block());

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void handleCallback_rejectsRedirectUriOutsideAllowList() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.handleCallback("code", null, "https://evil.com/callback"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void handleCallback_rejectsHostnameThatMerelyStartsWithAnAllowedOrigin() {
        // "https://app.example.com.evil.com" starts with the allowed origin string but is a
        // different host entirely; a naive startsWith check would wrongly accept it.
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.handleCallback("code", null, "https://app.example.com.evil.com/callback"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void getLoginUrl_fallsBackToDefaultRedirectUriWhenOmitted() {
        when(authService.getAuthorizationUrl("https://app.example.com/default-callback", null))
                .thenReturn("https://keycloak/auth?redirect_uri=default");

        var response = controller.getLoginUrl(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getUserInfo_propagatesKeycloakException() {
        when(authService.getUserInfo("invalid-token"))
                .thenReturn(Mono.error(new KeycloakException("Failed to fetch user info", HttpStatus.UNAUTHORIZED)));

        assertThrows(
                KeycloakException.class,
                () -> controller.getUserInfo("Bearer invalid-token").block());
    }

    @Test
    void getUserInfo_rejectsHeaderWithoutBearerScheme() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> controller.getUserInfo("invalid-token"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void login_stillReturnsTokenResponseOnSuccess() {
        LoginRequest request = LoginRequest.builder().email("user@example.com").password("correct").build();
        TokenResponse token = TokenResponse.builder().accessToken("abc").build();
        when(authService.login(request)).thenReturn(Mono.just(token));

        var response = controller.login(request).block();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(token, response.getBody());
    }
}
