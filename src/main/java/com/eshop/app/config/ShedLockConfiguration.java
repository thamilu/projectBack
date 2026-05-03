package com.eshop.app.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

/**
 * ShedLock Configuration for Distributed Scheduling
 * <p>
 * Ensures that scheduled tasks run only once in a clustered environment.
 * Prevents duplicate execution when multiple application instances are running.
 * <p>
 * <b>Database Table Required:</b>
 * <pre>
 * CREATE TABLE shedlock (
 *     name VARCHAR(64) PRIMARY KEY,
 *     lock_until TIMESTAMP(3) NOT NULL,
 *     locked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
 *     locked_by VARCHAR(255) NOT NULL
 * );
 * </pre>
 * <p>
 * <b>Usage:</b>
 * <pre>
 * {@code @Scheduled(cron = "0 0 2 * * *")}
 * {@code @SchedulerLock(
 *     name = "cleanupExpiredCarts",
 *     lockAtLeastFor = "PT5M",
 *     lockAtMostFor = "PT1H"
 * )}
 * public void cleanupExpiredCarts() {
 *     // Task implementation
 * }
 * </pre>
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class ShedLockConfiguration {
    
    /**
     * Create JDBC-based lock provider using PostgreSQL
     */
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .withTableName("shedlock")
                .usingDbTime() // Use database time for consistency across servers
                .build()
        );
    }
}
