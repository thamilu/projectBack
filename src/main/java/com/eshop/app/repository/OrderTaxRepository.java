package com.eshop.app.repository;

import com.eshop.app.entity.Order;
import com.eshop.app.entity.OrderTax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderTaxRepository extends JpaRepository<OrderTax, Long> {
    List<OrderTax> findByOrder(Order order);
}
