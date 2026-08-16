package com.eshop.app.catalog.domain.repository;

import com.eshop.app.catalog.domain.entity.MasterProduct;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MasterProductRepository
        extends JpaRepository<MasterProduct, Long>, JpaSpecificationExecutor<MasterProduct> {

    Optional<MasterProduct> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query(
            "SELECT mp FROM MasterProduct mp LEFT JOIN FETCH mp.brand LEFT JOIN FETCH mp.category"
                + " WHERE mp.active = true AND mp.approvalStatus ="
                + " com.eshop.app.catalog.domain.entity.MasterProductApprovalStatus.APPROVED AND"
                + " mp.productType = com.eshop.app.catalog.domain.entity.MasterProductType.MASTER")
    Page<MasterProduct> findAllActive(Pageable pageable);

    @Query(
            "SELECT mp FROM MasterProduct mp LEFT JOIN FETCH mp.brand LEFT JOIN FETCH mp.category"
                + " WHERE mp.active = true AND mp.approvalStatus ="
                + " com.eshop.app.catalog.domain.entity.MasterProductApprovalStatus.APPROVED AND"
                + " mp.productType = com.eshop.app.catalog.domain.entity.MasterProductType.MASTER"
                + " AND LOWER(mp.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<MasterProduct> searchActive(@Param("search") String search, Pageable pageable);
}
