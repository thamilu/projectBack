package com.eshop.app.controller;

import com.eshop.app.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component("appResponseBuilder")
@RequiredArgsConstructor
public class ResponseBuilder {

    public <T> ResponseEntity<ApiResponse<T>> buildCachedResponse(ApiResponse<T> payload, int cacheSeconds, boolean isPublic) {
        CacheControl cacheControl = isPublic
                ? CacheControl.maxAge(cacheSeconds, TimeUnit.SECONDS).cachePublic()
                : CacheControl.maxAge(cacheSeconds, TimeUnit.SECONDS).cachePrivate().noTransform();

        return ResponseEntity.ok()
                .cacheControl(cacheControl)
                .body(payload);
    }

    public <T> ResponseEntity<ApiResponse<T>> buildETaggedResponse(ApiResponse<T> payload, int cacheSeconds, String etag) {
        CacheControl cacheControl = CacheControl.maxAge(cacheSeconds, TimeUnit.SECONDS).cachePublic();
        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(cacheControl)
                .body(payload);
    }
}
