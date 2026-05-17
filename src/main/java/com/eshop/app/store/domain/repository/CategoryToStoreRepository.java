package com.eshop.app.store.domain.repository;

import com.eshop.app.catalog.domain.entity.Category;
import com.eshop.app.store.domain.entity.CategoryToStore;
import com.eshop.app.store.domain.entity.Store;














import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryToStoreRepository extends JpaRepository<CategoryToStore, Long> {
    List<CategoryToStore> findByStore(Store store);
    List<CategoryToStore> findByCategory(Category category);
    Optional<CategoryToStore> findByCategoryAndStore(Category category, Store store);
    List<CategoryToStore> findByStoreAndVisibleTrue(Store store);
}
