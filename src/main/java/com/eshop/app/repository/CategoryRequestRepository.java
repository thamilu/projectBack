package com.eshop.app.repository;

import com.eshop.app.entity.CategoryRequest;
import com.eshop.app.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRequestRepository extends JpaRepository<CategoryRequest, Long> {
    List<CategoryRequest> findByStatus(RequestStatus status);
    List<CategoryRequest> findBySellerId(Long sellerId);
    List<CategoryRequest> findBySellerIdAndStatus(Long sellerId, RequestStatus status);
    boolean existsByCategoryNameIgnoreCase(String categoryName);
}
