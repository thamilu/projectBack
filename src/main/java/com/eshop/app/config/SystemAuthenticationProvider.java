package com.eshop.app.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

/**
 * System Authentication Provider
 * 
 * <p><b>CRITICAL-003 FIX:</b> Provides system-level authentication context for scheduled tasks
 * and background jobs that need to call @PreAuthorize annotated methods.
 * 
 * <h2>Problem:</h2>
 * <pre>
 * Failed to warm top-selling products cache: An Authentication object was not found in the SecurityContext
 * org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
 * at com.eshop.app.scheduler.CacheWarmingScheduler.warmTopSellingProductsCache
 * </pre>
 * 
 * <h2>Root Cause:</h2>
 * <ul>
 *   <li>CacheWarmingScheduler calls service methods annotated with @PreAuthorize</li>
 *   <li>Scheduled tasks run without HTTP request context</li>
 *   <li>SecurityContext is empty, causing authorization to fail</li>
 * </ul>
 * 
 * <h2>Solution:</h2>
 * <ul>
 *   <li>Create synthetic SYSTEM user with required authorities</li>
 *   <li>Set SecurityContext before calling secured methods</li>
 *   <li>Clear SecurityContext after execution (restore original if existed)</li>
 * </ul>
 * 
 * <h2>Usage:</h2>
 * <pre>
 * &#64;Component
 * &#64;RequiredArgsConstructor
 * public class CacheWarmingScheduler {
 *     private final SystemAuthenticationProvider systemAuthProvider;
 *     private final ProductService productService;
 * 
 *     &#64;Scheduled(fixedDelay = 60000)
 *     public void warmCache() {
 *         systemAuthProvider.runAsSystem(() -> {
 *             productService.getTopSellingProducts(100);
 *             return null;
 *         });
 *     }
 * }
 * </pre>
 * 
 * <h2>Security Considerations:</h2>
 * <ul>
 *   <li>System user has ROLE_SYSTEM and ROLE_ADMIN authorities</li>
 *   <li>Only for internal background jobs - never exposed to external requests</li>
 *   <li>Audit logs should identify actions performed by SYSTEM user</li>
 * </ul>
 * 
 * @author EShop Security Team
 * @version 1.0
 * @since 2025-12-22
 */
@Component
@Slf4j
public class SystemAuthenticationProvider {

    private static final String SYSTEM_USER = "SYSTEM";
    
    private static final List<SimpleGrantedAuthority> SYSTEM_AUTHORITIES = List.of(
        new SimpleGrantedAuthority("ROLE_SYSTEM"),
        new SimpleGrantedAuthority("ROLE_ADMIN")
    );

    /**
     * Execute a task with system-level authentication.
     * 
     * <p>Temporarily sets SecurityContext to a system user with admin privileges,
     * executes the provided action, then restores the original context.
     * 
     * @param action the action to execute with system privileges
     * @param <T> the return type of the action
     * @return the result of the action
     */
    public <T> T runAsSystem(Supplier<T> action) {
        Authentication originalAuth = SecurityContextHolder.getContext().getAuthentication();
        
        try {
            // Create system authentication token
            Authentication systemAuth = new UsernamePasswordAuthenticationToken(
                SYSTEM_USER,
                null,
                SYSTEM_AUTHORITIES
            );
            
            // Set system context
            SecurityContextHolder.getContext().setAuthentication(systemAuth);
            log.trace("Executing task as SYSTEM user");
            
            // Execute the action
            return action.get();
            
        } finally {
            // Restore original context
            if (originalAuth != null) {
                SecurityContextHolder.getContext().setAuthentication(originalAuth);
                log.trace("Restored original authentication: {}", originalAuth.getName());
            } else {
                SecurityContextHolder.clearContext();
                log.trace("Cleared security context");
            }
        }
    }

    /**
     * Execute a runnable task with system-level authentication (void return).
     * 
     * @param action the runnable to execute with system privileges
     */
    public void runAsSystem(Runnable action) {
        runAsSystem(() -> {
            action.run();
            return null;
        });
    }

    /**
     * Check if current authentication is the system user.
     * 
     * @return true if current user is SYSTEM, false otherwise
     */
    public boolean isSystemUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && SYSTEM_USER.equals(auth.getName());
    }
}
