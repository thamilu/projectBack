package com.eshop.app.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Dynamic DataSource Router
 * 
 * Routes database connections based on transaction read-only status:
 * - Read-only transactions → Read replicas
 * - Read-write transactions → Primary database
 * 
 * Thread-safe routing using Spring's TransactionSynchronizationManager
 */
@Slf4j
public class RoutingDataSource extends AbstractRoutingDataSource {

    /**
     * Determine current datasource based on transaction context
     * 
     * @return DataSourceType.READ for read-only transactions, DataSourceType.WRITE otherwise
     */
    @Override
    protected Object determineCurrentLookupKey() {
        boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        
        DataSourceConfig.DataSourceType dataSourceType = isReadOnly 
            ? DataSourceConfig.DataSourceType.READ 
            : DataSourceConfig.DataSourceType.WRITE;
        
        if (log.isTraceEnabled()) {
            log.trace("Routing to {} datasource (readOnly={})", dataSourceType, isReadOnly);
        }
        
        return dataSourceType;
    }
}
