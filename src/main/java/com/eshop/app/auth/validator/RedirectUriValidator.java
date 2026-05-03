package com.eshop.app.auth.validator;

import com.eshop.app.auth.exception.InvalidRedirectUriException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Secure redirect URI validator with protection against:
 * - Open redirect attacks
 * - URL injection
 * - SSRF attempts
 * - Domain confusion attacks
 * 
 * Uses whitelist-based validation with proper URI parsing.
 */
@Component
@Slf4j
public class RedirectUriValidator {
    
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final Pattern VALID_HOST_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?)*$");
    private static final Pattern VALID_URI_PATTERN = 
        Pattern.compile("^https?://[a-zA-Z0-9][a-zA-Z0-9-.]*[a-zA-Z0-9](:[0-9]+)?(/.*)?$");
    
    @Value("${app.security.allowed-redirect-uris:}")
    private List<String> configuredRedirectUris;
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    private volatile ImmutableRedirectConfig config;
    
    @PostConstruct
    void init() {
        log.info("Initializing RedirectUriValidator");
        
        Set<String> exactMatches = new HashSet<>();
        List<RedirectPattern> wildcardPatterns = new ArrayList<>();
        
        if (configuredRedirectUris != null && !configuredRedirectUris.isEmpty()) {
            for (String uri : configuredRedirectUris) {
                if (!StringUtils.hasText(uri)) continue;
                
                String trimmed = uri.trim();
                
                // Handle comma-separated values
                if (trimmed.contains(",")) {
                    Arrays.stream(trimmed.split("\\s*,\\s*"))
                        .filter(StringUtils::hasText)
                        .forEach(u -> processUri(u, exactMatches, wildcardPatterns));
                } else {
                    processUri(trimmed, exactMatches, wildcardPatterns);
                }
            }
        }
        
        this.config = new ImmutableRedirectConfig(
            Collections.unmodifiableSet(exactMatches),
            List.copyOf(wildcardPatterns)
        );
        
        log.info("Initialized redirect validator with {} exact URIs and {} patterns",
                 exactMatches.size(), wildcardPatterns.size());
        log.debug("Allowed exact match URIs: {}", exactMatches);
    }
    
    private void processUri(String uri, Set<String> exactMatches, List<RedirectPattern> wildcardPatterns) {
        String normalized = normalizeUri(uri);
        
        if (normalized.contains("*")) {
            // Only allow path wildcards, not domain wildcards
            if (!normalized.contains("/") || normalized.indexOf('*') < normalized.lastIndexOf('/')) {
                log.warn("Skipping invalid wildcard pattern (must be in path): {}", uri);
                return;
            }
            wildcardPatterns.add(compilePattern(normalized));
        } else {
            exactMatches.add(normalized.toLowerCase());
        }
    }
    
    /**
     * Validates and normalizes a redirect URI.
     * 
     * @param redirectUri The URI to validate
     * @param clientIp Client IP for logging
     * @return Normalized URI if valid
     * @throws InvalidRedirectUriException if validation fails
     */
    public String validateAndNormalize(String redirectUri, String clientIp) {
        if (!StringUtils.hasText(redirectUri)) {
            throw new InvalidRedirectUriException("Redirect URI cannot be empty");
        }
        
        // Decode and sanitize
        String decoded = decodeUri(redirectUri);
        String normalized = normalizeUri(decoded);
        
        // Validate structure
        validateUriStructure(normalized);
        
        // Check against whitelist
        if (!isAllowed(normalized)) {
            log.warn("Rejected redirect URI '{}' from IP: {}", redirectUri, clientIp);
            throw new InvalidRedirectUriException("Redirect URI not allowed");
        }
        
        log.debug("Validated redirect URI: {}", normalized);
        return normalized;
    }
    
    /**
     * Checks if URI is allowed without throwing exception.
     */
    public boolean isAllowed(String uri) {
        if (!StringUtils.hasText(uri)) {
            return false;
        }
        
        // Validate URI format first
        if (!VALID_URI_PATTERN.matcher(uri).matches()) {
            log.debug("Rejected malformed redirect URI: {}", uri);
            return false;
        }
        
        String normalized = normalizeUri(uri).toLowerCase();
        ImmutableRedirectConfig currentConfig = this.config;
        
        // Check exact match first (O(1))
        if (currentConfig.exactMatches().contains(normalized)) {
            return true;
        }
        
        // Check wildcard patterns (secure matching)
        URI parsedUri;
        try {
            parsedUri = new URI(normalized);
        } catch (URISyntaxException e) {
            log.debug("Invalid URI syntax: {}", uri);
            return false;
        }
        
        return currentConfig.wildcardPatterns().stream()
            .anyMatch(pattern -> pattern.matches(parsedUri));
    }
    
    private String decodeUri(String uri) {
        try {
            // Prevent double encoding attacks
            String decoded = URLDecoder.decode(uri, StandardCharsets.UTF_8);
            if (!decoded.equals(uri)) {
                // Check for double encoding
                String doubleDecoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8);
                if (!doubleDecoded.equals(decoded)) {
                    throw new InvalidRedirectUriException("Double encoding detected");
                }
            }
            return decoded;
        } catch (IllegalArgumentException e) {
            throw new InvalidRedirectUriException("Invalid URI encoding");
        }
    }
    
    private void validateUriStructure(String uri) {
        URI parsed;
        try {
            parsed = new URI(uri);
        } catch (URISyntaxException e) {
            throw new InvalidRedirectUriException("Invalid URI syntax: " + e.getMessage());
        }
        
        // Validate scheme
        if (parsed.getScheme() == null || !ALLOWED_SCHEMES.contains(parsed.getScheme().toLowerCase())) {
            throw new InvalidRedirectUriException("Invalid URI scheme. Only http/https allowed");
        }
        
        // Validate host
        String host = parsed.getHost();
        if (host == null || !VALID_HOST_PATTERN.matcher(host).matches()) {
            throw new InvalidRedirectUriException("Invalid host");
        }
        
        // Prevent localhost in production
        if (isProductionEnvironment() && isLocalhost(host)) {
            throw new InvalidRedirectUriException("Localhost not allowed in production");
        }
        
        // Check for suspicious patterns
        if (uri.contains("@") || uri.contains("..") || countOccurrences(uri, "//") > 1) {
            throw new InvalidRedirectUriException("Suspicious URI pattern detected");
        }
    }
    
    private boolean isLocalhost(String host) {
        return host.equals("localhost") 
            || host.equals("127.0.0.1") 
            || host.startsWith("192.168.")
            || host.startsWith("10.")
            || host.startsWith("172.16.")
            || host.startsWith("172.17.")
            || host.startsWith("172.18.")
            || host.startsWith("172.19.")
            || host.startsWith("172.20.")
            || host.startsWith("172.21.")
            || host.startsWith("172.22.")
            || host.startsWith("172.23.")
            || host.startsWith("172.24.")
            || host.startsWith("172.25.")
            || host.startsWith("172.26.")
            || host.startsWith("172.27.")
            || host.startsWith("172.28.")
            || host.startsWith("172.29.")
            || host.startsWith("172.30.")
            || host.startsWith("172.31.")
            || host.equals("0.0.0.0");
    }
    
    private boolean isProductionEnvironment() {
        return "prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile);
    }
    
    private String normalizeUri(String uri) {
        String normalized = uri.trim();
        // Remove trailing slashes but preserve path structure
        while (normalized.length() > 1 && normalized.endsWith("/") && !normalized.endsWith("*/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
    
    private RedirectPattern compilePattern(String pattern) {
        int wildcardIndex = pattern.indexOf('*');
        String baseUrl = pattern.substring(0, wildcardIndex);
        
        try {
            URI baseUri = new URI(baseUrl);
            return new RedirectPattern(
                baseUri.getScheme(),
                baseUri.getHost(),
                baseUri.getPort(),
                baseUri.getPath() != null ? baseUri.getPath() : ""
            );
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid pattern URI: " + pattern, e);
        }
    }
    
    private int countOccurrences(String str, String substring) {
        int count = 0;
        int index = 0;
        while ((index = str.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
    
    /**
     * Immutable redirect configuration (thread-safe).
     */
    private record ImmutableRedirectConfig(
        Set<String> exactMatches,
        List<RedirectPattern> wildcardPatterns
    ) {
    }
    
    /**
     * Secure wildcard pattern for path-based matching.
     */
    private record RedirectPattern(
        String scheme,
        String host,
        int port,
        String pathPrefix
    ) {
        boolean matches(URI uri) {
            return scheme.equalsIgnoreCase(uri.getScheme())
                && host.equalsIgnoreCase(uri.getHost())
                && (port == -1 || port == uri.getPort())
                && (uri.getPath() != null && uri.getPath().startsWith(pathPrefix));
        }
    }
}
