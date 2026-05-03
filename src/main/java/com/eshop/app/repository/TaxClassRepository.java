package com.eshop.app.repository;

import com.eshop.app.entity.TaxClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxClassRepository extends JpaRepository<TaxClass, Long> {
    List<TaxClass> findByActiveTrue();
}
