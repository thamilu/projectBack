package com.eshop.app.catalog.application.port.in;

import com.eshop.app.catalog.api.request.CategoryRequestResponse;
import com.eshop.app.catalog.api.request.ReviewRequest;
import com.eshop.app.catalog.api.request.CategoryRequest;
import java.util.List;

/**
 * Inbound Port for Category Request Use Cases.
 */
public interface CategoryRequestUseCase {
    CategoryRequestResponse createRequest(CategoryRequest dto, Long sellerId);
    List<CategoryRequestResponse> getSellerRequests(Long sellerId);
    List<CategoryRequestResponse> getPendingRequests();
    CategoryRequestResponse reviewRequest(Long requestId, ReviewRequest dto, Long adminId);
}
