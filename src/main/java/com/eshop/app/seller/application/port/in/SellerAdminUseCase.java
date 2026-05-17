package com.eshop.app.seller.application.port.in;

import com.eshop.app.user.api.response.SellerProfileResponse;
import java.util.List;
import java.util.Map;

/**
 * Inbound Port for Seller Administration Use Cases.
 */
public interface SellerAdminUseCase {
    List<SellerProfileResponse> getPendingSellers();
    List<Map<String, String>> getBusinessTypes();
    void approveSeller(Long sellerId, String processedBy);
    void rejectSeller(Long sellerId, String rejectionReason, String processedBy);
    void syncSellerRole(Long id);
}
