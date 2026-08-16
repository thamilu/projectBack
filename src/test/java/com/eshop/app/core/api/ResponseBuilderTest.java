package com.eshop.app.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eshop.app.core.api.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

class ResponseBuilderTest {

    private final ResponseBuilder responseBuilder = new ResponseBuilder();

    @Test
    void buildCachedResponse_public_setsPublicCacheControl() {
        ApiResponse<String> payload = ApiResponse.success("data");

        ResponseEntity<ApiResponse<String>> response = responseBuilder.buildCachedResponse(payload, 60, true);

        assertEquals(CacheControl.maxAge(60, java.util.concurrent.TimeUnit.SECONDS).cachePublic().getHeaderValue(),
                response.getHeaders().getCacheControl());
        assertEquals(payload, response.getBody());
    }

    @Test
    void buildCachedResponse_private_setsPrivateNoTransformCacheControl() {
        ApiResponse<String> payload = ApiResponse.success("data");

        ResponseEntity<ApiResponse<String>> response = responseBuilder.buildCachedResponse(payload, 30, false);

        String cacheControl = response.getHeaders().getCacheControl();
        assertTrue(cacheControl.contains("private"));
        assertTrue(cacheControl.contains("no-transform"));
    }

    @Test
    void buildCachedResponse_rejectsNullPayload() {
        assertThrows(IllegalArgumentException.class, () -> responseBuilder.buildCachedResponse(null, 60, true));
    }

    @Test
    void buildCachedResponse_rejectsNegativeCacheSeconds() {
        ApiResponse<String> payload = ApiResponse.success("data");
        assertThrows(
                IllegalArgumentException.class, () -> responseBuilder.buildCachedResponse(payload, -1, true));
    }

    @Test
    void buildCachedResponse_allowsZeroCacheSeconds() {
        ApiResponse<String> payload = ApiResponse.success("data");
        ResponseEntity<ApiResponse<String>> response = responseBuilder.buildCachedResponse(payload, 0, true);
        assertEquals(payload, response.getBody());
    }

    @Test
    void buildETaggedResponse_threeArg_defaultsToPublicCaching() {
        ApiResponse<String> payload = ApiResponse.success("data");

        ResponseEntity<ApiResponse<String>> response =
                responseBuilder.buildETaggedResponse(payload, 120, "\"v1\"");

        assertEquals("\"v1\"", response.getHeaders().getETag());
        assertTrue(response.getHeaders().getCacheControl().contains("public"));
    }

    @Test
    void buildETaggedResponse_fourArg_supportsPrivateCaching() {
        ApiResponse<String> payload = ApiResponse.success("sensitive-data");

        ResponseEntity<ApiResponse<String>> response =
                responseBuilder.buildETaggedResponse(payload, 30, "\"v2\"", false);

        assertEquals("\"v2\"", response.getHeaders().getETag());
        String cacheControl = response.getHeaders().getCacheControl();
        assertTrue(cacheControl.contains("private"));
        assertTrue(cacheControl.contains("no-transform"));
    }

    @Test
    void buildETaggedResponse_rejectsNullEtag() {
        ApiResponse<String> payload = ApiResponse.success("data");
        assertThrows(
                IllegalArgumentException.class,
                () -> responseBuilder.buildETaggedResponse(payload, 60, null));
    }

    @Test
    void buildETaggedResponse_rejectsBlankEtag() {
        ApiResponse<String> payload = ApiResponse.success("data");
        assertThrows(
                IllegalArgumentException.class,
                () -> responseBuilder.buildETaggedResponse(payload, 60, "   "));
    }

    @Test
    void buildETaggedResponse_rejectsNullPayload() {
        assertThrows(
                IllegalArgumentException.class,
                () -> responseBuilder.buildETaggedResponse(null, 60, "\"v1\""));
    }

    @Test
    void buildETaggedResponse_rejectsNegativeCacheSeconds() {
        ApiResponse<String> payload = ApiResponse.success("data");
        assertThrows(
                IllegalArgumentException.class,
                () -> responseBuilder.buildETaggedResponse(payload, -5, "\"v1\""));
    }
}
