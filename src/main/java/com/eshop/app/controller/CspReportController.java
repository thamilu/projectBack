package com.eshop.app.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * CSP (Content Security Policy) Report Controller
 * 
 * <p><b>CRITICAL-004 FIX:</b> Endpoint for receiving Content Security Policy violation reports
 * from browsers. Must be publicly accessible as browsers send these reports without authentication.
 * 
 * <h2>Problem:</h2>
 * <pre>
 * Authentication failed for request: POST /csp/report
 * </pre>
 * 
 * <h2>Root Cause:</h2>
 * <ul>
 *   <li>CSP report endpoints were not excluded from authentication requirements</li>
 *   <li>Browsers send CSP reports without authentication headers</li>
 *   <li>Missing public endpoint configuration caused 401 Unauthorized</li>
 * </ul>
 * 
 * <h2>Solution:</h2>
 * <ul>
 *   <li>Added /csp/report to permitAll() in security configuration</li>
 *   <li>Log CSP violations for security monitoring</li>
 *   <li>Track violations using Micrometer metrics</li>
 * </ul>
 * 
 * <h2>CSP Header Configuration:</h2>
 * <pre>
 * Content-Security-Policy-Report-Only: 
 *   default-src 'self'; 
 *   script-src 'self' 'unsafe-inline'; 
 *   style-src 'self' 'unsafe-inline'; 
 *   report-uri /csp/report
 * </pre>
 * 
 * @author EShop Security Team
 * @version 1.0
 * @since 2025-12-22
 */
@RestController
@RequestMapping("/csp")
@RequiredArgsConstructor
@Slf4j
public class CspReportController {

    private final MeterRegistry meterRegistry;

    /**
     * Receive CSP violation reports from browsers.
     * 
     * <p>This endpoint must be publicly accessible (no authentication required)
     * as browsers send CSP reports automatically when violations occur.
     * 
     * @param report the CSP violation report
     * @param request the HTTP request
     * @return 204 No Content (standard for CSP reports)
     */
    @PostMapping(value = "/report", consumes = {"application/json", "application/csp-report"})
    public ResponseEntity<Void> handleCspReport(
            @RequestBody CspViolationReport report,
            HttpServletRequest request) {
        
        String violatedDirective = report.violatedDirective();
        String blockedUri = report.blockedUri();
        String sourceFile = report.sourceFile();
        Integer lineNumber = report.lineNumber();
        
        log.warn("🚨 CSP Violation: directive={}, blocked-uri={}, source={}:{}", 
            violatedDirective, 
            blockedUri, 
            sourceFile, 
            lineNumber);
        
        // Track CSP violations in metrics
        Counter.builder("security.csp.violations")
            .description("CSP violation reports received")
            .tags(Tags.of(
                "directive", sanitizeTag(violatedDirective),
                "blocked_uri_host", extractHost(blockedUri)
            ))
            .register(meterRegistry)
            .increment();
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Sanitize tag values to prevent metric explosion.
     */
    private String sanitizeTag(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        // Limit length and remove problematic characters
        return value.length() > 50 ? value.substring(0, 50) : value;
    }

    /**
     * Extract host from URI for metric tagging.
     */
    private String extractHost(String uri) {
        if (uri == null || uri.isBlank()) {
            return "unknown";
        }
        try {
            if (uri.startsWith("http://") || uri.startsWith("https://")) {
                java.net.URL url = java.net.URI.create(uri).toURL();
                return url.getHost();
            }
            return "inline";
        } catch (Exception e) {
            return "invalid";
        }
    }

    /**
     * CSP Violation Report DTO
     * 
     * <p>
     * Matches the JSON structure sent by browsers:
     * 
     * <pre>
     * {
     *   "csp-report": {
     *     "document-uri": "https://app.example.com/",
     *     "violated-directive": "script-src 'self'",
     *     "blocked-uri": "https://evil.com/malicious.js",
     *     "source-file": "https://app.example.com/app.js",
     *     "line-number": 42
     *   }
     * }
     * </pre>
     */
    public record CspViolationReport(
        @JsonProperty("csp-report") CspReportDetails details
    ) {
        public String violatedDirective() {
            return details != null ? details.violatedDirective() : "unknown";
        }
        public String blockedUri() {
            return details != null ? details.blockedUri() : "unknown";
        }
        public String sourceFile() {
            return details != null ? details.sourceFile() : "unknown";
        }
        public Integer lineNumber() {
            return details != null ? details.lineNumber() : 0;
        }
    }

    /**
     * CSP Report Details (nested structure)
     */
    public record CspReportDetails(
        @JsonProperty("violated-directive") String violatedDirective,
        @JsonProperty("blocked-uri") String blockedUri,
        @JsonProperty("source-file") String sourceFile,
        @JsonProperty("line-number") Integer lineNumber,
        @JsonProperty("document-uri") String documentUri
    ) {}
}
