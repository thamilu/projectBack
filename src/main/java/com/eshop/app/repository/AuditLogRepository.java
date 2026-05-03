package com.eshop.app.repository;

import com.eshop.app.entity.AuditLog;
import com.eshop.app.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Repository for AuditLog entity
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

        /**
         * Find audit logs by user identifier (external subject or stored identifier)
         */
        Page<AuditLog> findByUserIdentifier(String userIdentifier, Pageable pageable);

    /**
     * Find audit logs by action
     */
    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

    /**
     * Find audit logs by entity type and ID
     */
    Page<AuditLog> findByEntityTypeAndEntityId(
            String entityType,
            String entityId,
            Pageable pageable);

    /**
     * Find audit logs within date range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate")
    Page<AuditLog> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Find failed audit logs
     */
    Page<AuditLog> findBySuccessFalse(Pageable pageable);

    /**
     * Delete old audit logs
     */
    @Query("DELETE FROM AuditLog a WHERE a.timestamp < :cutoffDate")
    void deleteOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}

