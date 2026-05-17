package com.eshop.app.seller.domain.repository;

import com.eshop.app.seller.domain.entity.SellerFarmerDetails;




import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerFarmerDetailsRepository extends JpaRepository<SellerFarmerDetails, Long> {
    Optional<SellerFarmerDetails> findBySellerProfile_Id(Long sellerProfileId);
}
