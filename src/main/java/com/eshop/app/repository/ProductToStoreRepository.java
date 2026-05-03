package com.eshop.app.repository;

import com.eshop.app.entity.Product;
import com.eshop.app.entity.ProductToStore;
import com.eshop.app.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductToStoreRepository extends JpaRepository<ProductToStore, Long> {
    List<ProductToStore> findByStore(Store store);
    List<ProductToStore> findByProduct(Product product);
    Optional<ProductToStore> findByProductAndStore(Product product, Store store);
    List<ProductToStore> findByStoreAndVisibleTrue(Store store);
}
