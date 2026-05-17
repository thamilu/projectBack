package com.eshop.app.catalog.api.controller;

import com.eshop.app.core.kernel.ApiConstants;
import com.eshop.app.core.api.response.ApiInfoResponse;
import com.eshop.app.core.api.response.ApiResponse;
import com.eshop.app.catalog.application.port.in.HomeUseCase;
import com.eshop.app.core.api.ResponseBuilder;
import com.eshop.app.core.api.response.HomeResponse;
import com.eshop.app.core.api.response.WelcomeResponse;
import com.eshop.app.core.exception.infrastructure.ServiceException;
import com.eshop.app.core.exception.security.UnauthorizedException;
import org.springframework.security.core.Authentication;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Home Pages", description = "Home page and role-specific dashboard endpoints")
@RestController
@RequestMapping(ApiConstants.Endpoints.HOME)
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    private final HomeUseCase homeService;
    private final ResponseBuilder responseBuilder;

    private static final int PUBLIC_CACHE_SECONDS = 60;
    private static final int INFO_CACHE_SECONDS = 120;
    private static final int PRIVATE_CACHE_SECONDS = 30;

    @GetMapping
    @Operation(summary = "Get home page information (public)")
    public ResponseEntity<ApiResponse<WelcomeResponse>> getHome() {
        log.debug("Fetching public home page information");

        WelcomeResponse welcome = WelcomeResponse.builder()
                .message(ApiConstants.WELCOME_MESSAGE)
                .version(ApiConstants.API_VERSION)
                .timestamp(Instant.now())
                .documentation(WelcomeResponse.DocumentationLinks.builder()
                        .swaggerUi(ApiConstants.DOCUMENTATION_URL)
                        .openApiJson(ApiConstants.API_DOCS_PATH)
                        .openApiYaml(ApiConstants.API_DOCS_PATH + ".yaml")
                        .build())
                .links(ApiConstants.ENDPOINTS)
                .build();

        return responseBuilder.buildCachedResponse(ApiResponse.success(welcome), PUBLIC_CACHE_SECONDS, true);
    }

    @GetMapping("/info")
    @Operation(summary = "Get API information")
    public ResponseEntity<ApiResponse<ApiInfoResponse>> getApiInfo(WebRequest request) {
        String etag = generateETag();
        if (request.checkNotModified(etag)) {
            return ResponseEntity.status(304).build();
        }

        ApiInfoResponse info = ApiInfoResponse.builder()
                .name(ApiConstants.API_NAME)
                .version(ApiConstants.API_VERSION)
                .description(ApiConstants.API_DESCRIPTION)
                .features(ApiConstants.FEATURES)
                .endpoints(ApiConstants.ENDPOINTS)
                .timestamp(Instant.now())
                .build();

        return responseBuilder.buildETaggedResponse(ApiResponse.success(info), INFO_CACHE_SECONDS, etag);
    }

    @GetMapping(ApiConstants.Endpoints.HOME)
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get role-specific dashboard")
    public ResponseEntity<ApiResponse<HomeResponse>> getUserHome(Authentication authentication) {
        validateAuthentication(authentication);

        log.info("Fetching dashboard for user: {}", authentication.getName());

        HomeResponse response = homeService.getHomePageData(authentication);
        if (response == null) {
            log.error("HomeService returned null for user: {}",
                    authentication == null ? "anonymous" : authentication.getName());
            throw new ServiceException("Failed to load dashboard data");
        }

        return responseBuilder.buildCachedResponse(ApiResponse.success(response), PRIVATE_CACHE_SECONDS, false);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check endpoint", hidden = true)
    public ResponseEntity<ApiResponse<Object>> healthCheck() {
        var payload = java.util.Map.of(
                "status", "UP",
                "timestamp", Instant.now(),
                "version", ApiConstants.API_VERSION);
        return responseBuilder.buildCachedResponse(ApiResponse.success(payload), PUBLIC_CACHE_SECONDS, true);
    }

    private void validateAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthorized dashboard access attempt");
            throw new UnauthorizedException("Full authentication is required");
        }
    }

    private String generateETag() {
        return "\"" + ApiConstants.API_VERSION.hashCode() + "\"";
    }
}


