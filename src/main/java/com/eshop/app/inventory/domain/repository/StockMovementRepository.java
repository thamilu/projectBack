package com.eshop.app.inventory.domain.repository;

import com.eshop.app.inventory.domain.entity.StockMovement;




import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
}
