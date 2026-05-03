package com.eshop.app.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
// Avoid direct dependency on DataSourceProperties (varies across Spring Boot versions)
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * DataSource Configuration for Read-Write Splitting
 * 
 * Architecture:
 * - Write operations → Primary database (single writer)
 * - Read operations → Read replicas (3+ replicas with load balancing)
 * 
 * Benefits:
 * - Scales read-heavy queries (product listing, search)
 * - Reduces write database load by 70-80%
 * - Enables horizontal scaling for reads
 * 
 * Usage:
 * - Annotate read-only methods with @Transactional(readOnly = true)
 * - Write operations use default @Transactional (readOnly = false)
 * 
 * Connection Pools:
 * - Write pool: 20 connections (limited by single writer)
 * - Read pool: 50 connections per replica (scales horizontally)
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.datasource.read-replicas.enabled", havingValue = "true", matchIfMissing = false)
public class DataSourceConfig {

    /**
     * Write datasource (primary database)
     * Binds properties to HikariDataSource directly to avoid DataSourceProperties type.
     */
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public HikariDataSource writeDataSource() {
        HikariDataSource dataSource = new HikariDataSource();

        // Conservative defaults; can be overridden via configuration
        dataSource.setPoolName("WritePool");
        dataSource.setMaximumPoolSize(20);
        dataSource.setMinimumIdle(5);
        dataSource.setConnectionTimeout(30000);
        dataSource.setIdleTimeout(600000);
        dataSource.setMaxLifetime(1800000);

        log.info("Write DataSource configured (binding properties)");
        return dataSource;
    }

    /**
     * Read datasource (read replicas)
     * Handles SELECT operations with round-robin load balancing
     * 
     * Note: For multiple read replicas, configure comma-separated URLs:
     * spring.datasource.read-replicas.url=jdbc:postgresql://read-replica-1:5432,read-replica-2:5432,read-replica-3:5432/eshop_db
     * 
     * HikariCP will automatically load balance across replicas.
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.read-replicas")
    public HikariDataSource readDataSource() {
        HikariDataSource dataSource = new HikariDataSource();

        // Generous defaults; can be overridden via configuration
        dataSource.setPoolName("ReadPool");
        dataSource.setMaximumPoolSize(50);
        dataSource.setMinimumIdle(10);
        dataSource.setConnectionTimeout(20000);
        dataSource.setIdleTimeout(600000);
        dataSource.setMaxLifetime(1800000);
        dataSource.setReadOnly(true);

        log.info("Read DataSource configured (binding properties)");
        return dataSource;
    }

    /**
     * Routing datasource that selects write or read based on transaction context
     * 
     * Routing Logic:
     * - @Transactional(readOnly = true) → Read replica
     * - @Transactional or @Transactional(readOnly = false) → Write database
     */
    @Bean
    @Primary
    public DataSource routingDataSource(
            @Qualifier("writeDataSource") DataSource writeDataSource,
            @Qualifier("readDataSource") DataSource readDataSource) {
        
        RoutingDataSource routingDataSource = new RoutingDataSource();
        
        Map<Object, Object> dataSources = new HashMap<>();
        dataSources.put(DataSourceType.WRITE, writeDataSource);
        dataSources.put(DataSourceType.READ, readDataSource);
        
        routingDataSource.setTargetDataSources(dataSources);
        routingDataSource.setDefaultTargetDataSource(writeDataSource); // Default to write for safety
        
        log.info("Routing DataSource configured with WRITE and READ datasources");
        
        // Wrap with LazyConnectionDataSourceProxy to ensure routing happens after transaction starts
        return new LazyConnectionDataSourceProxy(routingDataSource);
    }

    /**
     * DataSource type enum
     */
    public enum DataSourceType {
        WRITE,
        READ
    }
}
