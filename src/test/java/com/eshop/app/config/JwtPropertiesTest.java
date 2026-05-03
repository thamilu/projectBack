package com.eshop.app.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.context.TestPropertySource;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("test")
@Import(TestOAuth2DisabledConfig.class)
@TestPropertySource(properties = {
        "app.security.admin.issuer-uri=http://localhost:8080/realms/eshop-admin"
})
class JwtPropertiesTest {

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void shouldLoadJwtProperties() {
        assertThat(jwtProperties).isNotNull();
        assertThat(jwtProperties.issuerUri()).isNotBlank();
        assertThat(jwtProperties.jwkSetUri()).isNotBlank();
        assertThat(jwtProperties.audiences()).isNotEmpty();
        assertThat(jwtProperties.authorityPrefix()).isEqualTo("ROLE_");
    }

    @Test
    void shouldNotAllowInsecureAlgorithms() {
        assertThat(jwtProperties.allowedAlgorithms()).doesNotContain("none", "HS256");
        assertThat(jwtProperties.allowedAlgorithms()).contains("RS256");
    }
}
