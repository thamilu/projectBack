package com.eshop.app.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eshop.app.core.exception.infrastructure.KeycloakException;
import com.eshop.app.user.api.request.LoginRequest;
import com.eshop.app.user.api.request.RefreshTokenRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static com.eshop.app.user.application.service.KeycloakAuthHelper.Operation.AUTHORIZATION_CODE;
import static com.eshop.app.user.application.service.KeycloakAuthHelper.Operation.LOGOUT;
import static com.eshop.app.user.application.service.KeycloakAuthHelper.Operation.PASSWORD_GRANT;
import static com.eshop.app.user.application.service.KeycloakAuthHelper.Operation.REFRESH_TOKEN;

class KeycloakAuthHelperTest {

    private KeycloakAuthHelper helper;

    @BeforeEach
    void setUp() {
        helper = new KeycloakAuthHelper(new ObjectMapper());
    }

    // ==================== maskEmail ====================

    @Test
    void maskEmail_nullInput_returnsPlaceholder() {
        assertThat(helper.maskEmail(null)).isEqualTo("***");
    }

    @Test
    void maskEmail_noAtSign_returnsPlaceholder() {
        assertThat(helper.maskEmail("not-an-email")).isEqualTo("***");
    }

    @Test
    void maskEmail_normalEmail_masksLocalPart() {
        assertThat(helper.maskEmail("john.doe@example.com")).isEqualTo("j***@example.com");
    }

    @Test
    void maskEmail_singleCharLocalPart_masksWithFirstCharVisible() {
        // Only an EMPTY local part collapses to a bare "*"; a 1-character local part still
        // goes through the normal "firstChar***@domain" path.
        assertThat(helper.maskEmail("j@example.com")).isEqualTo("j***@example.com");
    }

    @Test
    void maskEmail_leadingAtSign_doesNotThrow() {
        assertThat(helper.maskEmail("@example.com")).isEqualTo("*@example.com");
    }

    @Test
    void maskEmail_trailingAtSign_doesNotThrow() {
        assertThat(helper.maskEmail("user@")).doesNotContain("null");
    }

    @Test
    void maskEmail_multipleAtSigns_doesNotThrow() {
        // RFC 5321 edge case / typo input — must not throw, exact masking format is secondary.
        assertThat(helper.maskEmail("a@b@c")).isNotBlank();
    }

    // ==================== sanitizeErrorBody ====================

    @Test
    void sanitizeErrorBody_nullInput_returnsEmptyString() {
        assertThat(helper.sanitizeErrorBody(null)).isEmpty();
    }

    @Test
    void sanitizeErrorBody_blankInput_returnsEmptyString() {
        assertThat(helper.sanitizeErrorBody("   ")).isEmpty();
    }

    @Test
    void sanitizeErrorBody_redactsAccessToken() {
        String body = "{\"access_token\":\"secret-token-value\",\"token_type\":\"Bearer\"}";
        String sanitized = helper.sanitizeErrorBody(body);

        assertThat(sanitized).doesNotContain("secret-token-value");
        assertThat(sanitized).contains("[REDACTED]");
        assertThat(sanitized).contains("Bearer");
    }

    @Test
    void sanitizeErrorBody_redactsClientSecretAndPassword() {
        String body = "{\"client_secret\":\"super-secret\",\"password\":\"hunter2\",\"error\":\"invalid_grant\"}";
        String sanitized = helper.sanitizeErrorBody(body);

        assertThat(sanitized).doesNotContain("super-secret");
        assertThat(sanitized).doesNotContain("hunter2");
        assertThat(sanitized).contains("invalid_grant");
    }

    @Test
    void sanitizeErrorBody_emptyValueField_isStillRedacted() {
        // A regex requiring 1+ characters inside the quotes would miss this; structural
        // field-based redaction catches it regardless of value length.
        String body = "{\"access_token\":\"\"}";
        String sanitized = helper.sanitizeErrorBody(body);

        assertThat(sanitized).contains("[REDACTED]");
    }

    @Test
    void sanitizeErrorBody_whitespaceAroundColon_isStillRedacted() {
        String body = "{ \"access_token\" : \"secret\" }";
        String sanitized = helper.sanitizeErrorBody(body);

        assertThat(sanitized).doesNotContain("secret");
        assertThat(sanitized).contains("[REDACTED]");
    }

    @Test
    void sanitizeErrorBody_nonJsonBody_returnsPlaceholderNotRawContent() {
        String html = "<html><body>502 Bad Gateway from upstream proxy</body></html>";
        String sanitized = helper.sanitizeErrorBody(html);

        assertThat(sanitized).doesNotContain("502 Bad Gateway");
    }

    @Test
    void sanitizeErrorBody_noSensitiveFields_leavesContentIntact() {
        String body = "{\"error\":\"invalid_client\",\"error_description\":\"Client not found\"}";
        String sanitized = helper.sanitizeErrorBody(body);

        assertThat(sanitized).contains("invalid_client");
        assertThat(sanitized).contains("Client not found");
    }

    // ==================== parseErrorMessage ====================

    @Test
    void parseErrorMessage_nullBody_returnsGenericMessage() {
        assertThat(helper.parseErrorMessage(null, PASSWORD_GRANT)).isEqualTo("Authentication failed");
    }

    @Test
    void parseErrorMessage_invalidGrantForPasswordGrant_returnsCredentialsMessage() {
        String body = "{\"error\":\"invalid_grant\",\"error_description\":\"Invalid user credentials\"}";
        assertThat(helper.parseErrorMessage(body, PASSWORD_GRANT)).isEqualTo("Invalid email or password");
    }

    @Test
    void parseErrorMessage_invalidGrantForRefreshToken_returnsSessionExpiredMessage() {
        String body = "{\"error\":\"invalid_grant\"}";
        assertThat(helper.parseErrorMessage(body, REFRESH_TOKEN)).isEqualTo("Session expired. Please login again.");
    }

    @Test
    void parseErrorMessage_invalidGrantForAuthorizationCode_returnsExpiredCodeMessage() {
        String body = "{\"error\":\"invalid_grant\"}";
        assertThat(helper.parseErrorMessage(body, AUTHORIZATION_CODE))
                .isEqualTo("Invalid or expired authorization code");
    }

    @Test
    void parseErrorMessage_invalidClient_returnsClientConfigMessage() {
        String body = "{\"error\":\"invalid_client\"}";
        assertThat(helper.parseErrorMessage(body, PASSWORD_GRANT)).isEqualTo("Invalid client configuration");
    }

    @Test
    void parseErrorMessage_unknownErrorType_returnsGenericMessageWithoutThrowing() {
        String body = "{\"error\":\"some_future_keycloak_error_type\"}";
        assertThat(helper.parseErrorMessage(body, PASSWORD_GRANT))
                .isEqualTo("Authentication failed. Please try again.");
    }

    @Test
    void parseErrorMessage_malformedJson_doesNotThrow() {
        assertThat(helper.parseErrorMessage("not json at all {{{", PASSWORD_GRANT)).isEqualTo("Authentication failed");
    }

    // ==================== validate* ====================

    @Test
    void validateLoginRequest_nullRequest_throws() {
        assertThatThrownBy(() -> helper.validateLoginRequest(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateLoginRequest_blankEmail_throws() {
        LoginRequest request = LoginRequest.builder().email(" ").password("secret").build();
        assertThatThrownBy(() -> helper.validateLoginRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email");
    }

    @Test
    void validateLoginRequest_blankPassword_throws() {
        LoginRequest request = LoginRequest.builder().email("user@example.com").password("").build();
        assertThatThrownBy(() -> helper.validateLoginRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password");
    }

    @Test
    void validateLoginRequest_validRequest_doesNotThrow() {
        LoginRequest request = LoginRequest.builder().email("user@example.com").password("secret").build();
        assertThatCode(() -> helper.validateLoginRequest(request)).doesNotThrowAnyException();
    }

    @Test
    void validateRefreshTokenRequest_nullRequest_throws() {
        assertThatThrownBy(() -> helper.validateRefreshTokenRequest(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateRefreshTokenRequest_blankToken_throws() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("  ");
        assertThatThrownBy(() -> helper.validateRefreshTokenRequest(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateAuthorizationCodeRequest_blankCode_throws() {
        assertThatThrownBy(() -> helper.validateAuthorizationCodeRequest("", "https://app.example.com/callback"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("code");
    }

    @Test
    void validateAuthorizationCodeRequest_blankRedirectUri_throws() {
        assertThatThrownBy(() -> helper.validateAuthorizationCodeRequest("code123", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Redirect URI");
    }

    @Test
    void validateToken_blankToken_throws() {
        assertThatThrownBy(() -> helper.validateToken(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateToken_validToken_doesNotThrow() {
        assertThatCode(() -> helper.validateToken("abc.def.ghi")).doesNotThrowAnyException();
    }

    // ==================== mapToKeycloakException ====================

    @Test
    void mapToKeycloakException_alreadyKeycloakException_returnsSameInstance() {
        KeycloakException original = new KeycloakException("already mapped", HttpStatus.BAD_REQUEST);

        Throwable result = helper.mapToKeycloakException(original, PASSWORD_GRANT);

        assertThat(result).isSameAs(original);
    }

    @Test
    void mapToKeycloakException_callNotPermitted_mapsTo503() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("keycloak-test");
        CallNotPermittedException cnp = CallNotPermittedException.createCallNotPermittedException(circuitBreaker);

        Throwable result = helper.mapToKeycloakException(cnp, LOGOUT);

        assertThat(result).isInstanceOf(KeycloakException.class);
        assertThat(((KeycloakException) result).getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void mapToKeycloakException_timeout_mapsTo504NotClientTimeout() {
        // 504 (Gateway Timeout), not 408 (Request Timeout): this is OUR backend timing out
        // waiting on Keycloak (an upstream failure), not the caller being slow to send us
        // their request — 408 would be the wrong semantic here.
        Throwable result = helper.mapToKeycloakException(new TimeoutException("connect timed out"), PASSWORD_GRANT);

        assertThat(result).isInstanceOf(KeycloakException.class);
        assertThat(((KeycloakException) result).getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    }

    @Test
    void mapToKeycloakException_webClientResponseException401_preservesUnauthorized() {
        WebClientResponseException webEx = WebClientResponseException.create(
                401, "Unauthorized", new HttpHeaders(),
                "{\"error\":\"invalid_grant\"}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        Throwable result = helper.mapToKeycloakException(webEx, PASSWORD_GRANT);

        assertThat(result).isInstanceOf(KeycloakException.class);
        KeycloakException ke = (KeycloakException) result;
        assertThat(ke.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ke.getMessage()).isEqualTo("Invalid email or password");
    }

    @Test
    void mapToKeycloakException_webClientResponseException500_mapsTo503NotRaw500() {
        // Keycloak's own 5xx must not masquerade as THIS service's internal error.
        WebClientResponseException webEx = WebClientResponseException.create(
                500, "Internal Server Error", new HttpHeaders(),
                "{\"error\":\"server_error\"}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        Throwable result = helper.mapToKeycloakException(webEx, PASSWORD_GRANT);

        assertThat(result).isInstanceOf(KeycloakException.class);
        assertThat(((KeycloakException) result).getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void mapToKeycloakException_webClientResponseExceptionDoesNotLeakRawBodyInMessage() {
        WebClientResponseException webEx = WebClientResponseException.create(
                400, "Bad Request", new HttpHeaders(),
                "{\"error\":\"invalid_client\",\"error_description\":\"internal-client-id-xyz not found in realm internal-realm\"}"
                        .getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        Throwable result = helper.mapToKeycloakException(webEx, PASSWORD_GRANT);

        assertThat(((KeycloakException) result).getMessage())
                .doesNotContain("internal-client-id-xyz")
                .doesNotContain("internal-realm");
    }

    @Test
    void mapToKeycloakException_unexpectedException_mapsTo500() {
        Throwable result = helper.mapToKeycloakException(new RuntimeException("boom"), PASSWORD_GRANT);

        assertThat(result).isInstanceOf(KeycloakException.class);
        assertThat(((KeycloakException) result).getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
