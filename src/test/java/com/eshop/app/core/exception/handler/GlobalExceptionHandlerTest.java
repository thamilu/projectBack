package com.eshop.app.core.exception.handler;

import com.eshop.app.core.api.response.ApiError;
import com.eshop.app.core.exception.business.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = Mockito.mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }

    @Test
    @DisplayName("Should handle MaxUploadSizeExceededException and return CONTENT_TOO_LARGE (413)")
    void testHandleMaxUploadSizeExceeded() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(5000000L);

        ResponseEntity<ApiError> response = handler.handleMaxUploadSizeExceeded(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("FILE_TOO_LARGE");
    }

    @Test
    @DisplayName("Should sanitize sensitive field rejected values in ValidationException")
    void testSanitizeSensitiveRejectedValueInValidationException() {
        ValidationException ex = new ValidationException("Validation error");
        ex.addFieldError("userPassword", "too short", "mySecretPwd123");
        ex.addFieldError("email", "invalid format", "invalid-email");

        ResponseEntity<ApiError> response = handler.handleValidationException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();

        List<ApiError.FieldError> fieldErrors = response.getBody().fieldErrors();
        assertThat(fieldErrors).hasSize(2);

        // Sensitive field 'userPassword' must be redacted
        assertThat(fieldErrors.get(0).field()).isEqualTo("userPassword");
        assertThat(fieldErrors.get(0).rejectedValue()).isEqualTo("***REDACTED***");

        // Non-sensitive field 'email' retains original value
        assertThat(fieldErrors.get(1).field()).isEqualTo("email");
        assertThat(fieldErrors.get(1).rejectedValue()).isEqualTo("invalid-email");
    }

    @Test
    @DisplayName("Should sanitize sensitive field rejected values in MethodArgumentNotValidException")
    void testSanitizeSensitiveRejectedValueInMethodArgumentNotValid() {
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        FieldError passwordFieldError = new FieldError("user", "password", "superSecretToken123", false, null, null, "must be strong");
        FieldError nameFieldError = new FieldError("user", "username", "john_doe", false, null, null, "invalid");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(passwordFieldError, nameFieldError));

        MethodParameter methodParameter = Mockito.mock(MethodParameter.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ApiError> response = handler.handleMethodArgumentNotValid(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();

        List<ApiError.FieldError> fieldErrors = response.getBody().fieldErrors();
        assertThat(fieldErrors).hasSize(2);
        assertThat(fieldErrors.get(0).rejectedValue()).isEqualTo("***REDACTED***");
        assertThat(fieldErrors.get(1).rejectedValue()).isEqualTo("john_doe");
    }

    @Test
    @DisplayName("Should handle MethodArgumentTypeMismatchException with 400 status")
    void testHandleMethodArgumentTypeMismatch() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException("abc", Integer.class, "age", null, null);

        ResponseEntity<ApiError> response = handler.handleMethodArgumentTypeMismatch(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("TYPE_MISMATCH");
    }

    @Test
    @DisplayName("Should handle MissingServletRequestParameterException with 400 status")
    void testHandleMissingServletRequestParameter() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("page", "int");

        ResponseEntity<ApiError> response = handler.handleMissingServletRequestParameter(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("MISSING_PARAMETER");
    }

    @Test
    @DisplayName("Should handle HttpRequestMethodNotSupportedException with 405 status")
    void testHandleMethodNotSupported() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST", List.of("GET"));

        ResponseEntity<ApiError> response = handler.handleMethodNotSupported(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("METHOD_NOT_ALLOWED");
    }
}
