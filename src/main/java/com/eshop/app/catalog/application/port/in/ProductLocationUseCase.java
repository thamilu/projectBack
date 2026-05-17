package com.eshop.app.catalog.application.port.in;

import com.eshop.app.catalog.api.request.ProductLocationSearchRequest;
import com.eshop.app.catalog.api.response.ProductLocationResponse;
import org.springframework.data.domain.Page;

/**
 * Inbound Port for Product Location Use Cases.
 */
public interface ProductLocationUseCase {
    Page<ProductLocationResponse> searchProductsByLocation(ProductLocationSearchRequest request);
    Double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2);
    Double convertKmToMiles(Double kilometers);
    Double[] getUserLocationFromIp(String ipAddress);
    Page<ProductLocationResponse> searchProductsByStore(Long storeId, int page, int size);
}
