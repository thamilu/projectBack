package com.eshop.app.repository.analytics;


import com.eshop.app.entity.Order;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.HibernateHints.HINT_READ_ONLY;

/**
 * Optimized repository for analytics queries.
 * Implements batch processing and aggregation at database level.
 * 
 * @author EShop Team
 * @since 2.0
 */
@Repository
public interface AnalyticsOrderRepository extends JpaRepository<Order, Long> {
    
    /**
     * Calculates comprehensive order statistics in a single query.
     * Time Complexity: O(1) - single aggregation query
     * 
     * @param startDate start date for filtering
     * @param endDate end date for filtering
     * @return aggregated statistics
     */
    @Query("""
        SELECT new map(
            COUNT(o.id) as totalOrders,
            COALESCE(SUM(o.totalAmount), 0) as totalRevenue,
            COUNT(CASE WHEN o.orderStatus = 'PENDING' THEN 1 END) as pendingOrders,
            COUNT(CASE WHEN o.orderStatus = 'COMPLETED' THEN 1 END) as completedOrders,
            COUNT(CASE WHEN o.orderStatus = 'CANCELLED' THEN 1 END) as cancelledOrders,
            COUNT(DISTINCT o.customer.id) as uniqueCustomers
        )
        FROM Order o
        WHERE o.createdAt BETWEEN :startDate AND :endDate
        """)
    Map<String, Object> getOrderStatistics(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Gets seller-specific statistics in a single optimized query.
     * Prevents N+1 queries by aggregating all metrics at once.
     * 
     * @param sellerId the seller ID
     * @return seller statistics
     */
    @Query("""
        SELECT new map(
            COUNT(o.id) as totalOrders,
            COALESCE(SUM(o.totalAmount), 0) as totalRevenue,
            COUNT(CASE WHEN o.orderStatus = 'PENDING' THEN 1 END) as pendingOrders,
            COUNT(CASE WHEN o.orderStatus = 'COMPLETED' THEN 1 END) as completedOrders,
            COUNT(CASE WHEN o.orderStatus = 'CANCELLED' THEN 1 END) as cancelledOrders,
            COUNT(DISTINCT o.customer.id) as totalCustomers
        )
        FROM Order o
                    WHERE o.store.sellerProfile.user.id = :sellerId
        """)
    Map<String, Object> getSellerOrderStatistics(@Param("sellerId") Long sellerId);
    
    /**
     * Gets monthly revenue for a seller.
     * 
     * @param sellerId seller ID
     * @param startOfMonth start of current month
     * @return monthly revenue
     */
    @Query("""
        SELECT COALESCE(SUM(o.totalAmount), 0)
        FROM Order o
                    WHERE o.store.sellerProfile.user.id = :sellerId
            AND o.orderStatus = 'COMPLETED'
            AND o.createdAt >= :startOfMonth
        """)
    BigDecimal getMonthlyRevenueBySellerId(
            @Param("sellerId") Long sellerId,
            @Param("startOfMonth") LocalDateTime startOfMonth
    );
    
    /**
     * Gets daily sales data for analytics charts.
     * 
     * @param startDate start date
     * @param endDate end date
     * @return list of daily sales data
     */
    @Query("""
        SELECT new map(
            CAST(o.createdAt AS LocalDate) as date,
            COUNT(o.id) as orderCount,
            COALESCE(SUM(o.totalAmount), 0) as revenue
        )
        FROM Order o
        WHERE o.createdAt BETWEEN :startDate AND :endDate
            AND o.orderStatus = 'COMPLETED'
        GROUP BY CAST(o.createdAt AS LocalDate)
        ORDER BY CAST(o.createdAt AS LocalDate)
        """)
    List<Map<String, Object>> getDailySalesData(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Gets revenue breakdown by category.
     * 
     * @return map of category name to revenue
     */
    @Query("""
        SELECT new map(
            c.name as category,
            COALESCE(SUM(oi.subtotal), 0) as revenue
        )
        FROM OrderItem oi
        JOIN oi.product p
        JOIN p.category c
        JOIN oi.order o
        WHERE o.orderStatus = 'COMPLETED'
        GROUP BY c.name
        ORDER BY COALESCE(SUM(oi.subtotal), 0) DESC
        """)
    List<Map<String, Object>> getRevenueByCategory();
    
    /**
     * Streams orders for export without loading all into memory.
     * Uses cursor-based pagination for memory efficiency.
     * Space Complexity: O(1) - constant memory usage
     * 
     * @param startDate start date
     * @param endDate end date
     * @return stream of orders
     */
    @QueryHints(value = {
            @QueryHint(name = HINT_FETCH_SIZE, value = "1000"),
            @QueryHint(name = HINT_READ_ONLY, value = "true")
    })
    @Query("""
        SELECT o FROM Order o
        LEFT JOIN FETCH o.customer
            LEFT JOIN FETCH o.store
        WHERE o.createdAt BETWEEN :startDate AND :endDate
            AND o.orderStatus = 'COMPLETED'
        ORDER BY o.createdAt
        """)
    Stream<Order> streamCompletedOrders(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Finds orders with pagination and optimized fetching.
     * Prevents N+1 queries by eagerly fetching associations.
     * 
     * @param sellerId seller ID
     * @param pageable pagination parameters
     * @return page of orders
     */
    @Query("""
        SELECT DISTINCT o FROM Order o
                LEFT JOIN FETCH o.customer c
                LEFT JOIN FETCH c.userProfile
                    LEFT JOIN FETCH o.store s
                    LEFT JOIN FETCH s.sellerProfile sp
                    LEFT JOIN FETCH sp.user
        LEFT JOIN FETCH o.currency
                    WHERE sp.user.id = :sellerId
        ORDER BY o.createdAt DESC
        """)
    Page<Order> findBySellerIdWithAssociations(
            @Param("sellerId") Long sellerId,
            Pageable pageable
    );
    
    /**
     * Gets top customers by order count for a seller.
     * 
     * @param sellerId seller ID
     * @param limit max results
     * @return list of top customers
     */
    @Query("""
        SELECT new map(
            c.id as customerId,
                    up.firstName as firstName,
                    up.lastName as lastName,
                c.keycloakId as keycloakId,
            COUNT(o.id) as orderCount,
            COALESCE(SUM(o.totalAmount), 0) as totalSpent
        )
        FROM Order o
        JOIN o.customer c
                JOIN c.userProfile up
                    WHERE o.store.sellerProfile.user.id = :sellerId
            AND o.orderStatus = 'COMPLETED'
                GROUP BY c.id, up.firstName, up.lastName, c.keycloakId
        ORDER BY COUNT(o.id) DESC
        """)
    List<Map<String, Object>> getTopCustomersBySellerId(
            @Param("sellerId") Long sellerId,
            Pageable pageable
    );
}
