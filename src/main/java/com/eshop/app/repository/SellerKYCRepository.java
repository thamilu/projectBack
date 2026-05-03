package com.eshop.app.repository;

import com.eshop.app.entity.SellerKYC;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerKYCRepository extends JpaRepository<SellerKYC, Long> {
    Optional<SellerKYC> findBySellerProfile_Id(Long sellerProfileId);
    Optional<SellerKYC> findBySellerProfile_User_Id(Long userId);
    Optional<SellerKYC> findByPanNumber(String panNumber);
    Optional<SellerKYC> findByGstin(String gstin);
}
