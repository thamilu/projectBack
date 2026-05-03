package com.eshop.app.service;

import com.eshop.app.dto.response.SellerProfileResponse;
import java.util.List;
import java.util.Map;

/**
 * Enterprise-grade service for Seller Administration.
 * Handles approvals, rejections, and metadata management.
 */
public interface SellerAdminService {
    List<SellerProfileResponse> getPendingSellers();
    List<Map<String, String>> getBusinessTypes();
    void approveSeller(Long sellerId, String processedBy);
    void rejectSeller(Long sellerId, String rejectionReason, String processedBy);
    void syncSellerRole(Long id);
}
