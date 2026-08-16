package com.eshop.app.catalog.domain.repository;

import com.eshop.app.catalog.domain.entity.SellerProductImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SellerProductImageRepository extends JpaRepository<SellerProductImage, Long> {

    List<SellerProductImage> findByProductIdAndActiveTrue(Long productId);

    List<SellerProductImage> findByProductIdAndActiveTrueOrderBySortOrderAsc(Long productId);

    Optional<SellerProductImage> findByProductIdAndIsPrimaryTrueAndActiveTrue(Long productId);

    @Query(
            "SELECT spi FROM SellerProductImage spi WHERE spi.product.id = :productId AND"
                    + " spi.isPrimary = true AND spi.active = true")
    Optional<SellerProductImage> findPrimaryImageByProductId(@Param("productId") Long productId);

    boolean existsByProductIdAndIsPrimaryTrueAndActiveTrue(Long productId);
}
