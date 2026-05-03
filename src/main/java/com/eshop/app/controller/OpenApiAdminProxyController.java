package com.eshop.app.controller;

import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.eshop.app.constants.ApiConstants;
import com.eshop.app.config.properties.AppProperties;
/**
 * Lightweight proxy controller that returns the full OpenAPI JSON at
 * `/v3/api-docs/admin` by proxying the full generated `/v3/api-docs` output.
 * This is a non-invasive, non-production helper to ensure the Swagger UI
 * 'Admin API' entry shows API operations when grouped generation fails to
 * include paths.
 */
@Tag(name = "Admin Utilities", description = "Admin utility endpoints - OpenAPI proxy and debugging tools (dev only)")
@RestController
@RequestMapping(ApiConstants.Endpoints.ADMIN_PROBE)
@RequiredArgsConstructor
@Profile("!prod")
public class OpenApiAdminProxyController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final AppProperties appProperties;

    @GetMapping(value = "/admin", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> admin() {
        // Use the configured backend URL to proxy the OpenAPI docs (dev/test only)
        String backendUrl = appProperties.getBackendUrl();
        String url = backendUrl + ApiConstants.API_DOCS_PATH;
        String body = restTemplate.getForObject(url, String.class);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
