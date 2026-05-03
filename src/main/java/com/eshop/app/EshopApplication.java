package com.eshop.app;

import com.eshop.app.config.ApiInfoProperties;
import com.eshop.app.config.properties.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Main Spring Boot Application Entry Point.
 * 
 * <p><b>Enterprise Features Enabled:</b>
 * <ul>
 *   <li>@EnableCaching - Multi-layer caching with Caffeine</li>
 *   <li>@EnableRetry - Retry mechanism for transient failures</li>
 *   <li>@EnableAsync - Event-driven architecture with async processing</li>
 *   <li>@EnableConfigurationProperties - Externalized configuration with type-safe properties</li>
 * </ul>
 * 
 * <p><b>Spring Boot 4.0 Compatibility:</b>
 * <ul>
 *   <li>AppProperties - Type-safe binding for all app.* properties</li>
 *   <li>Configuration Processor - Generates metadata for IDE autocomplete</li>
 *   <li>Virtual Threads - Java 21 virtual threads support enabled</li>
 *   <li>Repository Configuration - Moved to dedicated @Configuration classes (JpaRepositoryConfig, RedisRepositoryConfig)</li>
 * </ul>
 * 
 * <p><b>CRITICAL-001 FIX:</b> Removed @EnableJpaRepositories from main class.
 * Repository scanning is now handled by:
 * <ul>
 *   <li>{@link com.eshop.app.config.JpaRepositoryConfig} - JPA repositories</li>
 *   <li>{@link com.eshop.app.config.RedisRepositoryConfig} - Redis repositories (conditional)</li>
 * </ul>
 * 
 * @author EShop Team
 * @version 2.0
 * @since 1.0
 * @see com.eshop.app.config.JpaRepositoryConfig
 * @see com.eshop.app.config.RedisRepositoryConfig
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class EshopApplication {

    public static void main(String[] args) {
        SpringApplication.run(EshopApplication.class, args);
    }

    @Bean
    public CommandLineRunner showLoadedProperties(AppProperties appProperties, ApiInfoProperties apiInfoProperties) {
        return args -> {
            System.out.println("AppProperties instance: " + appProperties);
            System.out.println("ApiInfoProperties instance: " + apiInfoProperties);
        };
    }

}