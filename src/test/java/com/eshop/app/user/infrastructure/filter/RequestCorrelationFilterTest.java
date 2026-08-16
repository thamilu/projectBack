package com.eshop.app.user.infrastructure.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void testFilter_GeneratesTraceIdWhenMissing() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("X-Trace-Id")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/v1/users");

        doAnswer(
                        invocation -> {
                            String mdcTraceId = MDC.get("traceId");
                            String mdcCorrelationId = MDC.get("correlationId");
                            String mdcUri = MDC.get("requestUri");

                            assertNotNull(mdcTraceId);
                            assertEquals(mdcTraceId, mdcCorrelationId);
                            assertEquals("/api/v1/users", mdcUri);
                            return null;
                        })
                .when(chain)
                .doFilter(request, response);

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader(eq("X-Trace-Id"), anyString());
        assertNull(MDC.get("traceId"));
    }

    @Test
    void testFilter_UsesExistingTraceIdHeader() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        String traceId = "existing-trace-id-12345";
        when(request.getHeader("X-Trace-Id")).thenReturn(traceId);
        when(request.getRequestURI()).thenReturn("/api/v1/users/me");

        doAnswer(
                        invocation -> {
                            assertEquals(traceId, MDC.get("traceId"));
                            assertEquals(traceId, MDC.get("correlationId"));
                            return null;
                        })
                .when(chain)
                .doFilter(request, response);

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader("X-Trace-Id", traceId);
        assertNull(MDC.get("traceId"));
    }
}
