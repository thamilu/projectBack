package com.eshop.app.core.exception.handler;

import com.eshop.app.core.api.response.ApiResponse;
import com.eshop.app.core.exception.business.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class DashboardExceptionHandlerTest {

    private DashboardExceptionHandler handler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        handler = new DashboardExceptionHandler();
        webRequest = Mockito.mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/test");
    }

    @Test
    @DisplayName("Should handle ValidationException and include field-level errors")
    void testHandleValidationExceptionWithFieldErrors() {
        ValidationException ex = new ValidationException("Validation failed");
        ex.addFieldError("email", "must not be blank");
        ex.addFieldError("age", "must be positive");

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError())
                .isEqualTo("Validation failed [email: must not be blank; age: must be positive]");
    }

    @Test
    @DisplayName("Should handle AccessDeniedException without leaking internal message")
    void testHandleAccessDeniedExceptionMasking() {
        AccessDeniedException ex = new AccessDeniedException("User role SCOPE_ADMIN missing internal table ID 42");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDeniedException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError())
                .isEqualTo("Access denied: you do not have permission to perform this action.");
        assertThat(response.getBody().getError()).doesNotContain("table ID 42");
    }
}
