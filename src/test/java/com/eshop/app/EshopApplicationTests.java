package com.eshop.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.eshop.app.config.TestSecurityConfig;
import com.eshop.app.config.TestOAuth2DisabledConfig;

@SpringBootTest(properties = {
	"spring.main.allow-bean-definition-overriding=true",
	"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
	"spring.flyway.enabled=false",
	"spring.security.oauth2.resourceserver.jwt.enabled=false",
	"server.port=0",
	"jwt.secret=test-secret",
	"jwt.expiration=3600000",
	"cache.warming.enabled=false",
	"app.swagger.enabled=false",
	"payment.enabled=false",
	"keycloak.enabled=false",
	"logging.structured.enabled=false",
	"image.storage.provider=local"
})
@Import({TestSecurityConfig.class, TestOAuth2DisabledConfig.class})
class EshopApplicationTests {

	@Test
	void contextLoads() {
	}

}
