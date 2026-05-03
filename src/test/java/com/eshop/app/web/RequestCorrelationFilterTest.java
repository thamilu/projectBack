package com.eshop.app.web;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RequestCorrelationFilterTest {

    @Test
    void filterAddsHeaderAndMdcWhenNoHeaderProvided() throws ServletException, IOException {
        RequestCorrelationFilter filter = new RequestCorrelationFilter();

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        jakarta.servlet.FilterChain chain = new jakarta.servlet.FilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
                // inside the chain the MDC should be present
                String inMdc = MDC.get(RequestCorrelationFilter.MDC_KEY);
                assertThat(inMdc).isNotBlank();
                // the response header should already be set by the filter before chain
                String hdr = ((MockHttpServletResponse) response).getHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER);
                assertThat(hdr).isEqualTo(inMdc);
            }
        };

        filter.doFilter(req, resp, chain);

        // after filter completes, response should have header and MDC should be cleared
        String header = resp.getHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER);
        assertThat(header).isNotBlank();
        assertThat(MDC.get(RequestCorrelationFilter.MDC_KEY)).isNull();
    }

    @Test
    void filterUsesProvidedHeaderAndMdcIsSet() throws ServletException, IOException {
        RequestCorrelationFilter filter = new RequestCorrelationFilter();

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        String provided = "my-id-123";
        req.addHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER, provided);

        jakarta.servlet.FilterChain chain = new jakarta.servlet.FilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
                String inMdc = MDC.get(RequestCorrelationFilter.MDC_KEY);
                assertThat(inMdc).isEqualTo(provided);
            }
        };

        filter.doFilter(req, resp, chain);

        assertThat(resp.getHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER)).isEqualTo(provided);
        assertThat(MDC.get(RequestCorrelationFilter.MDC_KEY)).isNull();
    }
}
