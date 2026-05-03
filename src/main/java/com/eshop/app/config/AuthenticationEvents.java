package com.eshop.app.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuthenticationEvents {

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent success) {
        Authentication auth = success.getAuthentication();
        String username = auth.getName();
        
        // Extract more details if available (e.g., from JWT)
        if (auth.getPrincipal() instanceof Jwt jwt) {
            username = jwt.getClaimAsString("preferred_username");
            if (username == null) {
                username = auth.getName();
            }
        }

        log.info("AUTHENTICATION SUCCESS: User '{}' successfully authenticated. Authorities: {}", 
                username, auth.getAuthorities());
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent failures) {
        log.warn("AUTHENTICATION FAILED: {}", failures.getException().getMessage());
    }
}
