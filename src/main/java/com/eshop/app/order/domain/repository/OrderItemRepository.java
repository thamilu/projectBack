package com.eshop.app.order.domain.repository;

import com.eshop.app.order.domain.entity.OrderItem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Collection;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.product.id = :productId")
    List<OrderItem> findByProductId(@Param("productId") Long productId);

    /**
     * Count active order items for a product (for deletion validation).
     * Uses COUNT query for performance instead of loading all items.
     */
    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.product.id = :productId AND oi.order.orderStatus IN :statuses")
    long countByProductIdAndOrderStatusIn(@Param("productId") Long productId,
            @Param("statuses") Collection<com.eshop.app.order.domain.entity.Order.OrderStatus> statuses);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.product.store.id = :storeId")
    List<OrderItem> findByStoreId(@Param("storeId") Long storeId);

    /** Whether the given seller has at least one item in the given order — drives order/shipping ownership checks. */
    @Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END FROM OrderItem oi "
            + "WHERE oi.order.id = :orderId AND oi.product.store.sellerProfile.user.id = :sellerId")
    boolean existsByOrderIdAndSellerId(@Param("orderId") Long orderId, @Param("sellerId") Long sellerId);
}
