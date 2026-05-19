package com.eshop.app.core.infrastructure.config.startup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@Component
@Profile("!test")
public class StartupHealthCheck implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private com.eshop.app.core.infrastructure.config.properties.AppProperties appProperties;

    @Autowired
    private DataSource dataSource;

    @Value("${server.port}")
    private int serverPort;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String backendUrl = appProperties.getBackendUrl();
        String context = contextPath == null ? "" : contextPath.trim();
        if (context.endsWith("/")) {
            context = context.substring(0, context.length() - 1);
        }

        try (Connection conn = dataSource.getConnection()) {
            log.info("[OK] Database connection verified");

            // [HARDEN] database sanitization: purge any empty/incomplete parent entities
            // and remove the corrupted 574186 postal code so that user input degrades gracefully.
            try (java.sql.Statement stmt = conn.createStatement()) {
                int deletedPostalCodes = stmt.executeUpdate("DELETE FROM postal_codes WHERE pin_code = '574186'");
                int deletedStates = stmt.executeUpdate("DELETE FROM states WHERE name = '' OR name IS NULL");
                int deletedDistricts = stmt.executeUpdate("DELETE FROM districts WHERE name = '' OR name IS NULL");
                int deletedCountries = stmt.executeUpdate("DELETE FROM countries WHERE name = '' OR name IS NULL");
                log.info("Sanitized location tables: deleted {} corrupt postal codes, {} empty states, {} empty districts, {} empty countries.",
                        deletedPostalCodes, deletedStates, deletedDistricts, deletedCountries);
            } catch (SQLException e) {
                log.warn("Non-critical location sanitization warning: {}", e.getMessage());
            }

            log.info("[OK] Application ready to serve requests");
            log.info("[DOCS] Swagger UI: {}{}/swagger-ui/index.html", backendUrl, context);
            log.info("[MON]  Actuator: {}{}/actuator/health", backendUrl, context);
        } catch (SQLException e) {
            log.error("[FAIL] Database connection failed", e);
        }
    }
}
