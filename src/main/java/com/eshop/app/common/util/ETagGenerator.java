package com.eshop.app.common.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Enterprise-grade ETag generator for HTTP conditional requests.
 * Supports strong and weak ETags with SHA-256 hashing.
 */
@Component
public class ETagGenerator {
    
    private static final String ETAG_FORMAT = "\"%s\"";
    
    /**
     * Generate strong ETag using SHA-256 hash of components.
     */
    public String generate(Object... components) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            for (Object component : components) {
                if (component != null) {
                    digest.update(component.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            
            byte[] hash = digest.digest();
            String hex = HexFormat.of().formatHex(hash).substring(0, 16);
            return String.format(ETAG_FORMAT, hex);
            
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
    
    /**
     * Generate ETag for versioned entity (optimized for JPA @Version).
     */
    public String forVersionedEntity(Long id, Long version) {
        return String.format(ETAG_FORMAT, id + "-" + version);
    }
    
    /**
     * Generate ETag for timestamped entity.
     */
    public String forTimestampedEntity(Long id, Instant updatedAt) {
        long timestamp = updatedAt != null ? updatedAt.toEpochMilli() : 0;
        return String.format(ETAG_FORMAT, id + "-" + timestamp);
    }
    
    /**
     * Generate weak ETag for approximate matching.
     */
    public String generateWeak(Object... components) {
        String strong = generate(components);
        return "W/" + strong;
    }
    
    /**
     * Parse and validate ETag header.
     */
    public Optional<String> parse(String etagHeader) {
        if (etagHeader == null || etagHeader.isBlank()) {
            return Optional.empty();
        }
        
        String cleaned = etagHeader.trim();
        if (cleaned.startsWith("W/")) {
            cleaned = cleaned.substring(2);
        }
        
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            return Optional.of(cleaned);
        }
        
        return Optional.empty();
    }
    
    /**
     * Check if ETags match (handles weak comparison).
     */
    public boolean matches(String clientEtag, String serverEtag, boolean weakComparison) {
        if (clientEtag == null || serverEtag == null) {
            return false;
        }
        
        if (weakComparison) {
            return stripWeakPrefix(clientEtag).equals(stripWeakPrefix(serverEtag));
        }
        
        return clientEtag.equals(serverEtag);
    }
    
    private String stripWeakPrefix(String etag) {
        return etag.startsWith("W/") ? etag.substring(2) : etag;
    }
}
