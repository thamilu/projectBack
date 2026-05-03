package com.eshop.app.repository;

import com.eshop.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Enhanced UserRepository with optimized statistics queries.
 * 
 * @author EShop Team
 * @since 2.0
 */
@Repository
public interface UserRepositoryEnhanced extends JpaRepository<User, Long> {
    
    /**
     * Gets aggregated user statistics in a single query.
     * 
     * @return map containing user counts by role and status
     */
    @Query("""
        SELECT new map(
            COUNT(u.id) as totalUsers,
            COUNT(CASE WHEN u.role = 'CUSTOMER' THEN 1 END) as totalCustomers,
            COUNT(CASE WHEN u.role = 'SELLER' THEN 1 END) as totalSellers,
                    COUNT(CASE WHEN u.role = 'DELIVERY_AGENT' THEN 1 END) as totalDeliveryAgents
        )
        FROM User u
        """)
    Map<String, Object> getUserStatistics();
    
    /**
     * Counts new users created this month.
     * 
     * @param startOfMonth start of current month
     * @return count of new users
     */
    @Query("""
        SELECT COUNT(u.id)
        FROM User u
        WHERE u.createdAt >= :startOfMonth
        """)
    Long countNewUsersSince(LocalDateTime startOfMonth);
}
