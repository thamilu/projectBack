package com.eshop.app.repository;

import com.eshop.app.entity.SellerWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerWalletRepository extends JpaRepository<SellerWallet, Long> {
    
    Optional<SellerWallet> findByUserId(Long userId);
}
