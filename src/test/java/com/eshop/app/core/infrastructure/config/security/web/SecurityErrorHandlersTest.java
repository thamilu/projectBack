package com.eshop.app.core.infrastructure.config.security.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

class SecurityErrorHandlersTest {

    private SimpleMeterRegistry meterRegistry;
    private SecurityErrorHandlers handlers;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        // Mirrors JsonConfig's production JsonMapper bean: JavaTimeModule + UTC default
        // timezone. The UTC timezone is required for ApiError.timestamp's JsonFormat
        // pattern to resolve an offset for a zoneless Instant.
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        objectMapper.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        handlers = new SecurityErrorHandlers(objectMapper, meterRegistry);
    }

    @Test
    void authenticationEntryPoint_returns401WithJsonUtf8() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handlers.authenticationEntryPoint().commence(
                request, response, new InsufficientAuthenticationException("no token"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json").contains("UTF-8");
        assertThat(response.getContentAsString()).contains("\"status\":401");
    }

    @Test
    void authenticationEntryPoint_setsWwwAuthenticateHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handlers.authenticationEntryPoint().commence(
                request, response, new InsufficientAuthenticationException("no token"));

        assertThat(response.getHeader("WWW-Authenticate")).contains("Bearer");
    }

    @Test
    void authenticationEntryPoint_skipsWritingWhenResponseAlreadyCommitted() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setCommitted(true);

        assertThatCode(() -> handlers.authenticationEntryPoint().commence(
                        request, response, new InsufficientAuthenticationException("no token")))
                .doesNotThrowAnyException();
        assertThat(response.getContentAsString()).isEmpty();
    }

    @Test
    void authenticationEntryPoint_incrementsMetric() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handlers.authenticationEntryPoint().commence(
                request, response, new InsufficientAuthenticationException("no token"));

        assertThat(meterRegistry.counter("security.http.error", "status", "401").count()).isEqualTo(1.0);
    }

    @Test
    void accessDeniedHandler_returns403WithJsonUtf8() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/admin/users/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handlers.accessDeniedHandler().handle(request, response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).contains("application/json").contains("UTF-8");
        assertThat(response.getContentAsString()).contains("\"status\":403");
    }

    @Test
    void accessDeniedHandler_responseBodyDoesNotLeakInternalExceptionMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/admin/users/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handlers.accessDeniedHandler().handle(request, response, new AccessDeniedException("internal-detail"));

        String body = response.getContentAsString();
        assertThat(body).contains("You do not have permission");
        assertThat(body).doesNotContain("internal-detail");
    }

    @Test
    void accessDeniedHandler_skipsWritingWhenResponseAlreadyCommitted() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/admin/users/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setCommitted(true);

        assertThatCode(() -> handlers.accessDeniedHandler().handle(
                        request, response, new AccessDeniedException("denied")))
                .doesNotThrowAnyException();
        assertThat(response.getContentAsString()).isEmpty();
    }

    @Test
    void accessDeniedHandler_incrementsMetric() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/admin/users/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handlers.accessDeniedHandler().handle(request, response, new AccessDeniedException("denied"));

        assertThat(meterRegistry.counter("security.http.error", "status", "403").count()).isEqualTo(1.0);
    }

    @Test
    void accessDeniedHandler_withNoAuthenticationInContext_doesNotThrow() throws Exception {
        // No SecurityContext populated — exercises the anonymous/unauthenticated branch
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/admin/users/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatCode(() -> handlers.accessDeniedHandler().handle(
                        request, response, new AccessDeniedException("denied")))
                .doesNotThrowAnyException();
        assertThat(response.getStatus()).isEqualTo(403);
    }
}
