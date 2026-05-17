package com.eshop.app.seller.application.port.in;

import com.eshop.app.seller.api.response.SellerDashboardResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Inbound Port for Seller Dashboard Use Cases.
 */
public interface SellerDashboardUseCase {
    SellerDashboardResponse getDashboard(Long sellerId);
    CompletableFuture<SellerDashboardResponse> getDashboardAsync(Long sellerId);
}
