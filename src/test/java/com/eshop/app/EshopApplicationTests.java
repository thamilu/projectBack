package com.eshop.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.eshop.app.core.infrastructure.config.security.TestSecurityConfig;
import com.eshop.app.core.infrastructure.config.security.TestOAuth2DisabledConfig;

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
	"image.storage.provider=local",
	"app.seed.enabled=false"
})
@Import({TestSecurityConfig.class, TestOAuth2DisabledConfig.class})
class EshopApplicationTests {

	@Test
	void testPostalCodeParsing() {
		try {
			java.io.File file = new java.io.File("src/main/resources/seed/postal_codes.json");
			System.out.println("TESTING POSTAL CODE PARSING. File exists: " + file.exists());
			if (!file.exists()) return;

			com.fasterxml.jackson.core.JsonFactory factory = new com.fasterxml.jackson.core.JsonFactory();
			com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

			try (java.io.InputStream is = new java.io.FileInputStream(file);
				 java.io.Reader reader = new com.eshop.app.seed.seeders.RobustJsonReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8));
				 com.fasterxml.jackson.core.JsonParser parser = factory.createParser(reader)) {
				if (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.START_ARRAY) {
					System.out.println("Not a start array");
					return;
				}

				long count = 0;
				while (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.END_ARRAY) {
					if (parser.getCurrentToken() == com.fasterxml.jackson.core.JsonToken.START_OBJECT) {
						com.eshop.app.seed.seeders.PostalCodeRow row = 
							mapper.readValue(parser, com.eshop.app.seed.seeders.PostalCodeRow.class);
						count++;
						
						// Emulate resolution logic to see if any throws error
						try {
							String iso = row.isoCode() != null ? row.isoCode() : "IN";
							String countryName = row.countryName() != null ? row.countryName().trim() : "India";
							
							// Verify non-null/non-empty and resolve unused variable warnings
							if (iso.isEmpty() || countryName.isEmpty()) {
								throw new IllegalStateException("ISO or Country Name is empty");
							}
							
							String stateName = row.stateName();
							if (stateName == null) {
								throw new NullPointerException("stateName is null");
							}

							String districtName = row.districtName();
							if (districtName == null) {
								throw new NullPointerException("districtName is null");
							}
						} catch (Exception e) {
							System.err.println("CRITICAL ERROR at row " + count + ": " + row);
							e.printStackTrace();
							throw e;
						}
					}
				}
				System.out.println("Successfully parsed " + count + " rows without logic exception.");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
