package com.eshop.app.core.api.controller;

import com.eshop.app.core.kernel.ApiConstants;
import com.eshop.app.core.api.response.WelcomeResponse;
import com.eshop.app.core.api.response.ApiStatusResponse;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.view.RedirectView;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * API Documentation and Information Controller
 *
 * Provides welcome page, API information, and documentation redirects.
 * All URLs are dynamically generated based on the request context.
 *
 * SECURITY NOTE: Base URL construction honors X-Forwarded-* headers for
 * reverse-proxy deployments. These headers are attacker-controllable unless
 * a trusted-proxy filter (e.g. Spring's ForwardedHeaderFilter with a
 * configured trusted proxy list, or Tomcat's RemoteIpValve) is enforced at
 * the infrastructure layer. This controller applies defense-in-depth
 * validation (scheme whitelist, hostname format check, port range check)
 * but does NOT replace proper trusted-proxy configuration.
 */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
@io.swagger.v3.oas.annotations.tags.Tag(name = "API Documentation", description = "API information and documentation navigation")
public class ApiDocumentationController {

    // Constants for documentation paths
    private static final String SWAGGER_UI_PATH = "/swagger-ui/index.html";
    private static final String OPENAPI_SPEC_PATH = "/v3/api-docs";
    private static final String OPENAPI_YAML_PATH = "/v3/api-docs.yaml";

    // Cache settings
    private static final int WELCOME_CACHE_SECONDS = 300; // 5 minutes
    private static final int STATUS_CACHE_SECONDS = 30; // 30 seconds

    // Forwarded-header handling (defense-in-depth validation)
    private static final String FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";
    private static final String FORWARDED_HOST_HEADER = "X-Forwarded-Host";
    private static final String FORWARDED_PORT_HEADER = "X-Forwarded-Port";
    private static final int MAX_HOST_HEADER_LENGTH = 253; // RFC 1035 max hostname length
    private static final Pattern SAFE_HOST_PATTERN = Pattern.compile("^[a-zA-Z0-9.-]+$");

    private final Optional<BuildProperties> buildProperties;
    private final Environment environment;

    @Value("${spring.application.name:EShop API}")
    private String applicationName;

    @Value("${app.description:E-Commerce REST API}")
    private String applicationDescription;

    @Value("${app.show-tech-details:false}")
    private boolean showTechDetails;

    // ==================== MAIN ENDPOINTS ====================

    @GetMapping("/")
    @Operation(summary = "API Welcome", description = "Welcome page with API documentation links and basic information")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Welcome information retrieved successfully")
    public ResponseEntity<WelcomeResponse> welcome(
            HttpServletRequest request,
            WebRequest webRequest) {

        log.debug("Welcome endpoint accessed from: {}", request.getRemoteAddr());

        String baseUrl = buildBaseUrl(request);
        String etag = generateWelcomeETag(baseUrl);

        // Support conditional requests (304 Not Modified).
        // checkNotModified() already writes status/headers to the underlying
        // HttpServletResponse; returning null here follows Spring's documented
        // contract and avoids double/duplicate status handling.
        if (webRequest.checkNotModified(etag)) {
            log.debug("Returning 304 Not Modified for welcome endpoint");
            return null;
        }

        WelcomeResponse response = buildWelcomeResponse(baseUrl);

        return ResponseEntity.ok()
                .eTag(etag)
                .varyBy(FORWARDED_HOST_HEADER, FORWARDED_PROTO_HEADER, FORWARDED_PORT_HEADER)
                .cacheControl(CacheControl.maxAge(WELCOME_CACHE_SECONDS, TimeUnit.SECONDS).cachePublic())
                .body(response);
    }

    @GetMapping("/status")
    @Operation(summary = "API Status", description = "Lightweight API liveness status including uptime; for comprehensive dependency health checks use /actuator/health")
    public ResponseEntity<ApiStatusResponse> getStatus(HttpServletRequest request) {
        log.debug("Status endpoint accessed");

        ApiStatusResponse response = ApiStatusResponse.builder()
                .status("UP")
                .version(getVersion())
                .uptime(getFormattedUptime())
                .uptimeMillis(getUptimeMillis())
                .timestamp(Instant.now())
                .profile(getActiveProfile())
                .build();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(STATUS_CACHE_SECONDS, TimeUnit.SECONDS).cachePrivate())
                .body(response);
    }

    @GetMapping("/info")
    @Operation(summary = "Detailed API Information", description = "Comprehensive API information including all available documentation links")
    public ResponseEntity<Map<String, Object>> getDetailedInfo(HttpServletRequest request) {
        String baseUrl = buildBaseUrl(request);

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", applicationName);
        info.put("description", applicationDescription);
        info.put("version", getVersion());
        info.put("status", "Running");
        info.put("timestamp", Instant.now());

        // Documentation links
        Map<String, String> documentation = new LinkedHashMap<>();
        documentation.put("swagger_ui", baseUrl + SWAGGER_UI_PATH);
        documentation.put("openapi_json", baseUrl + OPENAPI_SPEC_PATH);
        documentation.put("openapi_yaml", baseUrl + OPENAPI_YAML_PATH);
        info.put("documentation", documentation);

        // Endpoints summary
        Map<String, String> endpoints = new LinkedHashMap<>();
        endpoints.put("auth", baseUrl + ApiConstants.Endpoints.AUTH);
        endpoints.put("users", baseUrl + ApiConstants.Endpoints.USERS);
        endpoints.put("products", baseUrl + ApiConstants.Endpoints.PRODUCTS);
        endpoints.put("categories", baseUrl + ApiConstants.Endpoints.CATEGORIES);
        endpoints.put("orders", baseUrl + ApiConstants.Endpoints.ORDERS);
        info.put("endpoints", endpoints);

        // Technical details (only in development/when enabled)
        if (showTechDetails || isDevelopmentProfile()) {
            Map<String, String> technical = new LinkedHashMap<>();
            technical.put("java_version", System.getProperty("java.version"));
            technical.put("spring_boot_version", SpringBootVersion.getVersion());
            technical.put("active_profile", getActiveProfile());
            info.put("technical", technical);
        }

        return ResponseEntity.ok()
                .varyBy(FORWARDED_HOST_HEADER, FORWARDED_PROTO_HEADER, FORWARDED_PORT_HEADER)
                .cacheControl(CacheControl.maxAge(WELCOME_CACHE_SECONDS, TimeUnit.SECONDS).cachePublic())
                .body(info);
    }

    // ==================== REDIRECT ENDPOINTS ====================

    @GetMapping({ "/api", "/docs", "/swagger" })
    @Hidden
    @Operation(summary = "Redirect to Swagger UI", hidden = true)
    public RedirectView redirectToSwagger(HttpServletRequest request) {
        log.debug("Redirecting to Swagger UI from: {}", request.getRequestURI());

        RedirectView redirect = new RedirectView(SWAGGER_UI_PATH);
        redirect.setContextRelative(true);
        redirect.setExposeModelAttributes(false);
        return redirect;
    }

    @GetMapping("/api-docs")
    @Hidden
    public RedirectView redirectToApiDocs() {
        RedirectView redirect = new RedirectView(OPENAPI_SPEC_PATH);
        redirect.setContextRelative(true);
        redirect.setExposeModelAttributes(false);
        return redirect;
    }

    // ==================== HELPER METHODS ====================

    private WelcomeResponse buildWelcomeResponse(String baseUrl) {
        return WelcomeResponse.builder()
                .message("Welcome to " + applicationName)
                .version(getVersion())
                .description(applicationDescription)
                .status("Running")
                .timestamp(Instant.now())
                .documentation(WelcomeResponse.DocumentationLinks.builder()
                        .swaggerUi(baseUrl + SWAGGER_UI_PATH)
                        .openApiJson(baseUrl + OPENAPI_SPEC_PATH)
                        .openApiYaml(baseUrl + OPENAPI_YAML_PATH)
                        .build())
                .links(buildQuickLinks(baseUrl))
                .build();
    }

    private Map<String, String> buildQuickLinks(String baseUrl) {
        Map<String, String> links = new LinkedHashMap<>();
        links.put("self", baseUrl + "/");
        links.put("status", baseUrl + "/status");
        links.put("info", baseUrl + "/info");
        links.put("health", baseUrl + "/actuator/health");
        return links;
    }

    /**
     * Builds the externally-visible base URL for this request.
     * <p>
     * Forwarded headers (X-Forwarded-Proto/Host/Port) are validated with a
     * strict allow-list before use, to reduce the risk of host-header
     * injection / cache poisoning when this application sits behind an
     * untrusted or misconfigured proxy. This is defense-in-depth only —
     * infrastructure-level trusted-proxy enforcement is still required
     * (see class-level Javadoc).
     */
    private String buildBaseUrl(HttpServletRequest request) {
        StringBuilder url = new StringBuilder();

        String forwardedProto = sanitizeScheme(request.getHeader(FORWARDED_PROTO_HEADER));
        String forwardedHost = sanitizeHost(request.getHeader(FORWARDED_HOST_HEADER));
        Integer forwardedPort = sanitizePort(request.getHeader(FORWARDED_PORT_HEADER));

        if (forwardedHost != null) {
            // Behind reverse proxy
            String scheme = forwardedProto != null ? forwardedProto : "https";
            url.append(scheme).append("://").append(forwardedHost);

            boolean hostIncludesPort = forwardedHost.indexOf(':') > -1;
            if (!hostIncludesPort && forwardedPort != null && !isDefaultPort(scheme, forwardedPort)) {
                url.append(":").append(forwardedPort);
            }
        } else {
            // Direct access
            String scheme = request.getScheme();
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();

            url.append(scheme).append("://").append(serverName);

            if (!isDefaultPort(scheme, serverPort)) {
                url.append(":").append(serverPort);
            }
        }

        url.append(request.getContextPath());
        return url.toString();
    }

    /**
     * Whitelists the forwarded scheme to http/https only, preventing
     * arbitrary scheme injection into generated documentation links.
     */
    private String sanitizeScheme(String scheme) {
        if (scheme == null) {
            return null;
        }
        String normalized = scheme.trim().toLowerCase();
        return ("http".equals(normalized) || "https".equals(normalized)) ? normalized : null;
    }

    /**
     * Validates the forwarded host against a strict allow-list pattern.
     * Rejects malformed/oversized values instead of reflecting them
     * unchecked into cacheable response bodies.
     * <p>
     * Known limitation: IPv6 literal hosts (e.g. "[::1]") are not matched
     * by this pattern and will safely fall back to the direct request
     * values; this is an accepted trade-off favoring strict validation.
     */
    private String sanitizeHost(String host) {
        if (host == null || host.isBlank() || host.length() > MAX_HOST_HEADER_LENGTH) {
            return null;
        }

        String hostPart = host;
        String portPart = null;
        int colonIndex = host.lastIndexOf(':');
        if (colonIndex > -1) {
            hostPart = host.substring(0, colonIndex);
            portPart = host.substring(colonIndex + 1);
        }

        if (!SAFE_HOST_PATTERN.matcher(hostPart).matches()) {
            log.warn("Rejected X-Forwarded-Host header due to invalid format");
            return null;
        }

        if (portPart != null && sanitizePort(portPart) == null) {
            log.warn("Rejected X-Forwarded-Host header due to invalid embedded port");
            return null;
        }

        return host;
    }

    /**
     * Safely parses a forwarded port value, never throwing on malformed
     * input. Valid TCP port range is 1-65535.
     */
    private Integer sanitizePort(String port) {
        if (port == null || port.isBlank()) {
            return null;
        }
        try {
            int value = Integer.parseInt(port.trim());
            return (value >= 1 && value <= 65535) ? value : null;
        } catch (NumberFormatException ex) {
            log.warn("Rejected non-numeric forwarded port header value");
            return null;
        }
    }

    private boolean isDefaultPort(String scheme, int port) {
        return ("http".equals(scheme) && port == 80) ||
                ("https".equals(scheme) && port == 443);
    }

    private String getVersion() {
        return buildProperties
                .map(BuildProperties::getVersion)
                .orElse("development");
    }

    /**
     * ETag now incorporates the resolved base URL, since the welcome
     * response content (documentation links) varies by host/scheme/port,
     * not just by application version and profile.
     */
    private String generateWelcomeETag(String baseUrl) {
        String version = getVersion();
        String profile = getActiveProfile();
        int hash = (version + profile + baseUrl).hashCode();
        return "\"welcome-" + hash + "\"";
    }

    private String getActiveProfile() {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length > 0 ? String.join(",", profiles) : "default";
    }

    private boolean isDevelopmentProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("dev") ||
                        p.equalsIgnoreCase("development") ||
                        p.equalsIgnoreCase("local"));
    }

    private long getUptimeMillis() {
        return ManagementFactory.getRuntimeMXBean().getUptime();
    }

    private String getFormattedUptime() {
        Duration uptime = Duration.ofMillis(getUptimeMillis());
        long days = uptime.toDays();
        long hours = uptime.toHoursPart();
        long minutes = uptime.toMinutesPart();
        long seconds = uptime.toSecondsPart();

        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
