package com.eshop.app.auth.service;

import com.eshop.app.auth.validator.RedirectUriValidator;
import com.eshop.app.config.KeycloakConfigProperties;
import com.eshop.app.config.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Service for handling logout operations.
 * Generates secure logout URLs with validated redirect URIs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogoutService {
    
    private final KeycloakConfigProperties keycloakConfig;
    private final RedirectUriValidator redirectUriValidator;
    private final AppProperties appProperties;
    
    /**
     * Generates a complete Keycloak logout URL with validated redirect.
     * 
     * @param requestedRedirectUri The URI to redirect to after logout (optional)
     * @param clientIp Client IP address for security logging
     * @return Complete logout URL
     */
    public String generateLogoutUrl(String requestedRedirectUri, String clientIp) {
        String validatedRedirectUri = getValidatedRedirectUri(requestedRedirectUri, clientIp);
        
        String logoutUrl = UriComponentsBuilder
            .fromUriString(keycloakConfig.getLogoutUrl())
            .queryParam("client_id", keycloakConfig.getResource())
            .queryParam("post_logout_redirect_uri", validatedRedirectUri)
            .build()
            .toUriString();
        
        log.debug("Generated logout URL with redirect to: {}", validatedRedirectUri);
        return logoutUrl;
    }
    
    /**
     * Validates redirect URI or returns default.
     */
    private String getValidatedRedirectUri(String redirectUri, String clientIp) {
        String defaultRedirectUri = appProperties.getSecurity().getDefaultRedirectUri();

        if (!StringUtils.hasText(redirectUri)) {
            log.debug("No redirect URI provided, using default: {}", defaultRedirectUri);
            return defaultRedirectUri;
        }
        
        return redirectUriValidator.validateAndNormalize(redirectUri, clientIp);
    }
}
