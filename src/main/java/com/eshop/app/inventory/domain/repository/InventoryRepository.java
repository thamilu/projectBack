package com.eshop.app.inventory.domain.repository;

import com.eshop.app.inventory.domain.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductId(Long productId);

    /**
     * Locks the inventory row for the duration of the transaction. Required for
     * reserve/confirm-sale under concurrent checkout — a plain read-then-write here is exactly
     * the "lost update" race the project standards forbid for inventory (optimistic {@code
     * @Version} alone only turns the race into random 409s under contention, it doesn't serialize
     * the decrement).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId")
    Optional<Inventory> findByProductIdForUpdate(@Param("productId") Long productId);
}
