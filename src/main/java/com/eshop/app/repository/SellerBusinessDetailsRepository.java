package com.eshop.app.repository;

import com.eshop.app.entity.SellerBusinessDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerBusinessDetailsRepository extends JpaRepository<SellerBusinessDetails, Long> {
    Optional<SellerBusinessDetails> findBySellerProfile_Id(Long sellerProfileId);
}
