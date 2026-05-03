package com.eshop.app.repository;

import com.eshop.app.entity.SellerWholesaleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerWholesaleConfigRepository extends JpaRepository<SellerWholesaleConfig, Long> {
    Optional<SellerWholesaleConfig> findBySellerProfile_Id(Long sellerProfileId);
}
