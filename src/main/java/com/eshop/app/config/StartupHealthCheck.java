package com.eshop.app.config;

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
    private com.eshop.app.config.properties.AppProperties appProperties;

    @Autowired
    private DataSource dataSource;

    @Value("${server.port}")
    private int serverPort;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String backendUrl = appProperties.getBackendUrl();
        try (Connection conn = dataSource.getConnection()) {
            log.info("[OK] Database connection verified");
            log.info("[OK] Application ready to serve requests");
            log.info("[DOCS] Swagger UI: {}/swagger-ui/index.html", backendUrl);
            log.info("[MON]  Actuator: {}/actuator/health", backendUrl);
        } catch (SQLException e) {
            log.error("[FAIL] Database connection failed", e);
        }
    }
}
