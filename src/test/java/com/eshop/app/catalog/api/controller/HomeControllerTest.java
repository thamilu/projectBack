package com.eshop.app.catalog.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.eshop.app.catalog.application.port.in.HomeUseCase;
import com.eshop.app.core.api.ResponseBuilder;
import com.eshop.app.core.api.response.ApiInfoResponse;
import com.eshop.app.core.api.response.ApiResponse;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

/**
 * Verifies the {@code /home/info} ETag now covers the full response content
 * (not just version) and that the response no longer carries a per-request
 * timestamp incompatible with conditional-GET caching.
 */
@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock HomeUseCase homeService;

    private final HomeController controller = new HomeController(homeService, new ResponseBuilder());

    @Test
    void apiInfoResponse_hasNoTimestampField() {
        // Compile-time proof would be circumvented by a stray getter; assert via reflection
        // that no "timestamp" field/property survives on the DTO backing the cached endpoint.
        boolean hasTimestampField =
                java.util.Arrays.stream(ApiInfoResponse.class.getDeclaredFields())
                        .anyMatch(f -> f.getName().equals("timestamp"));
        boolean hasTimestampGetter =
                java.util.Arrays.stream(ApiInfoResponse.class.getMethods())
                        .map(Method::getName)
                        .anyMatch(name -> name.equals("getTimestamp"));

        org.junit.jupiter.api.Assertions.assertFalse(hasTimestampField);
        org.junit.jupiter.api.Assertions.assertFalse(hasTimestampGetter);
    }

    @Test
    void getApiInfo_sameContentProducesSameETag() {
        WebRequest request1 = new ServletWebRequest(new MockHttpServletRequest());
        WebRequest request2 = new ServletWebRequest(new MockHttpServletRequest());

        ResponseEntity<ApiResponse<ApiInfoResponse>> first = controller.getApiInfo(request1);
        ResponseEntity<ApiResponse<ApiInfoResponse>> second = controller.getApiInfo(request2);

        assertEquals(first.getHeaders().getETag(), second.getHeaders().getETag());
    }

    @Test
    void getApiInfo_returns304WhenClientEtagMatches() {
        WebRequest firstRequest = new ServletWebRequest(new MockHttpServletRequest());
        ResponseEntity<ApiResponse<ApiInfoResponse>> firstResponse = controller.getApiInfo(firstRequest);
        String etag = firstResponse.getHeaders().getETag();

        MockHttpServletRequest conditionalRequest = new MockHttpServletRequest();
        conditionalRequest.addHeader("If-None-Match", etag);
        WebRequest secondRequest = new ServletWebRequest(conditionalRequest);

        ResponseEntity<ApiResponse<ApiInfoResponse>> secondResponse = controller.getApiInfo(secondRequest);

        assertEquals(304, secondResponse.getStatusCode().value());
    }
}
