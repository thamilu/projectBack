package com.eshop.app.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.mockito.Mockito.mock;

/**
 * Test configuration to disable OAuth2 and provide mock beans
 */
@TestConfiguration
public class TestOAuth2DisabledConfig {

    @Bean
    public JwtDecoder jwtDecoder() {
        // Provide a mock JwtDecoder to prevent OAuth2 autoconfiguration from failing
        return mock(JwtDecoder.class);
    }
}
