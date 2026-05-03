package com.eshop.app.service;

import com.eshop.app.dto.request.ProductLocationSearchRequest;
import com.eshop.app.dto.response.ProductLocationResponse;
import org.springframework.data.domain.Page;

/**
 * Service interface for location-based product search operations
 */
public interface ProductLocationService {

    /**
     * Search products based on user's location and filters
     * 
     * @param request Search request with location and filters
     * @return Page of products with distance information
     */
    Page<ProductLocationResponse> searchProductsByLocation(ProductLocationSearchRequest request);

    /**
     * Calculate distance between two geographical points using Haversine formula
     * 
     * @param lat1 Latitude of point 1
     * @param lon1 Longitude of point 1
     * @param lat2 Latitude of point 2
     * @param lon2 Longitude of point 2
     * @return Distance in kilometers
     */
    Double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2);

    /**
     * Convert kilometers to miles
     * 
     * @param kilometers Distance in kilometers
     * @return Distance in miles
     */
    Double convertKmToMiles(Double kilometers);

    /**
     * Get user's location from Google Geolocation API
     * (Requires Google API key)
     * 
     * @param ipAddress User's IP address
     * @return Array [latitude, longitude]
     */
    Double[] getUserLocationFromIp(String ipAddress);

    // New: search by store id (simple wrapper)
    org.springframework.data.domain.Page<com.eshop.app.dto.response.ProductLocationResponse> searchProductsByStore(
            Long storeId, int page, int size);
}
