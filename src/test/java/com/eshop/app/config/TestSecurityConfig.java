package com.eshop.app.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public JwtDecoder jwtDecoder() {
        // Simple stub JwtDecoder for tests — not used to actually decode tokens in unit tests.
        return new JwtDecoder() {
            @Override
            public Jwt decode(String token) throws JwtException {
                throw new JwtException("Test JwtDecoder stub should not be called");
            }
        };
    }
}
