package com.eshop.app.repository;

import com.eshop.app.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * Enhanced StoreRepository with statistics queries.
 * 
 * @author EShop Team
 * @since 2.0
 */
@Repository
public interface StoreRepositoryEnhanced extends JpaRepository<Store, Long> {

    /**
     * Gets aggregated store statistics.
     * 
     * @return map containing store counts
     */
    @Query("""
            SELECT new map(
                COUNT(s.id) as totalShops,
                COUNT(CASE WHEN s.active = true THEN 1 END) as activeShops
            )
            FROM Store s
            WHERE s.deleted = false
            """)
    Map<String, Object> getStoreStatistics();
}
