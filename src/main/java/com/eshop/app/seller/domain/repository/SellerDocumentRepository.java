package com.eshop.app.seller.domain.repository;

import com.eshop.app.seller.domain.entity.SellerDocument;




import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SellerDocumentRepository extends JpaRepository<SellerDocument, Long> {
    List<SellerDocument> findBySellerProfile_Id(Long sellerProfileId);
}
