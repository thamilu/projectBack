package com.eshop.app.seller.domain.repository;

import com.eshop.app.seller.domain.entity.SellerBankAccount;




import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SellerBankAccountRepository extends JpaRepository<SellerBankAccount, Long> {
    List<SellerBankAccount> findBySellerProfile_Id(Long sellerProfileId);
}
