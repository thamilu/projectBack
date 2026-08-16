package com.eshop.app.core.api;

import com.eshop.app.core.api.response.ApiResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * Builds {@link ResponseEntity} instances with standardized HTTP caching headers
 * (Cache-Control, ETag) for use across controllers.
 *
 * <p>Stateless and thread-safe.
 */
@Component("appResponseBuilder")
public class ResponseBuilder {

    /**
     * Build a {@code 200 OK} response with a Cache-Control header.
     *
     * @param payload      response body; must not be {@code null}
     * @param cacheSeconds max-age in seconds; must not be negative
     * @param isPublic     {@code true} for shared/public caching (e.g. CDN), {@code false} for
     *                     private, client-only caching of user-specific data
     * @return the built response entity
     * @throws IllegalArgumentException if {@code payload} is {@code null} or
     *                                   {@code cacheSeconds} is negative
     */
    public <T> ResponseEntity<ApiResponse<T>> buildCachedResponse(
            ApiResponse<T> payload, int cacheSeconds, boolean isPublic) {
        Assert.notNull(payload, "payload must not be null");
        Assert.isTrue(cacheSeconds >= 0, "cacheSeconds must not be negative");

        CacheControl cacheControl = resolveCacheControl(cacheSeconds, isPublic);

        return ResponseEntity.ok()
                .cacheControl(cacheControl)
                .body(payload);
    }

    /**
     * Build a {@code 200 OK} response with a public Cache-Control header and an ETag.
     *
     * <p>Delegates to {@link #buildETaggedResponse(ApiResponse, int, String, boolean)} with
     * {@code isPublic = true}. Preserved for backward compatibility with existing callers.
     *
     * @param payload      response body; must not be {@code null}
     * @param cacheSeconds max-age in seconds; must not be negative
     * @param etag         entity tag value; must not be {@code null} or blank
     * @return the built response entity
     * @throws IllegalArgumentException if {@code payload} is {@code null}, {@code etag} is
     *                                   {@code null}/blank, or {@code cacheSeconds} is negative
     */
    public <T> ResponseEntity<ApiResponse<T>> buildETaggedResponse(
            ApiResponse<T> payload, int cacheSeconds, String etag) {
        return buildETaggedResponse(payload, cacheSeconds, etag, true);
    }

    /**
     * Build a {@code 200 OK} response with a Cache-Control header and an ETag, with explicit
     * public/private cache scoping.
     *
     * <p>Use {@code isPublic = false} for ETagged responses containing user-specific or
     * otherwise sensitive data, to prevent shared/intermediary caches (CDNs, reverse proxies)
     * from storing the response.
     *
     * @param payload      response body; must not be {@code null}
     * @param cacheSeconds max-age in seconds; must not be negative
     * @param etag         entity tag value; must not be {@code null} or blank
     * @param isPublic     {@code true} for shared/public caching, {@code false} for private
     * @return the built response entity
     * @throws IllegalArgumentException if {@code payload} is {@code null}, {@code etag} is
     *                                   {@code null}/blank, or {@code cacheSeconds} is negative
     */
    public <T> ResponseEntity<ApiResponse<T>> buildETaggedResponse(
            ApiResponse<T> payload, int cacheSeconds, String etag, boolean isPublic) {
        Assert.notNull(payload, "payload must not be null");
        Assert.isTrue(StringUtils.hasText(etag), "etag must not be null or blank");
        Assert.isTrue(cacheSeconds >= 0, "cacheSeconds must not be negative");

        CacheControl cacheControl = resolveCacheControl(cacheSeconds, isPublic);

        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(cacheControl)
                .body(payload);
    }

    private CacheControl resolveCacheControl(int cacheSeconds, boolean isPublic) {
        CacheControl base = CacheControl.maxAge(cacheSeconds, TimeUnit.SECONDS);
        return isPublic ? base.cachePublic() : base.cachePrivate().noTransform();
    }
}
