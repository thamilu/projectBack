package com.eshop.app.service.impl;

import com.eshop.app.dto.response.ProductLocationResponse;
import com.eshop.app.service.ProductLocationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductLocationServiceImpl implements ProductLocationService {

    private final EntityManager entityManager;

    @Override
    public Page<ProductLocationResponse> searchProductsByLocation(
            com.eshop.app.dto.request.ProductLocationSearchRequest request) {
        double lat = request.getLatitude();
        double lon = request.getLongitude();
        double radius = request.getRadiusKm() != null ? request.getRadiusKm() : 10.0;

        String sql = "SELECT p.id as product_id, p.name as product_name, p.price as price, s.id as store_id, s.store_name as store_name, s.latitude as store_lat, s.longitude as store_lon, "
                +
                "(6371 * acos(cos(radians(:lat)) * cos(radians(s.latitude)) * cos(radians(s.longitude) - radians(:lon)) + sin(radians(:lat)) * sin(radians(s.latitude)))) as distance_km "
                +
                "FROM products p JOIN stores s ON p.store_id = s.id " +
                "WHERE (6371 * acos(cos(radians(:lat)) * cos(radians(s.latitude)) * cos(radians(s.longitude) - radians(:lon)) + sin(radians(:lat)) * sin(radians(s.latitude)))) <= :radius "
                +
                "ORDER BY distance_km ASC LIMIT :limit OFFSET :offset";

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("lat", lat);
        q.setParameter("lon", lon);
        q.setParameter("radius", radius);
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;
        q.setParameter("limit", size);
        q.setParameter("offset", page * size);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<ProductLocationResponse> results = new ArrayList<>();
        for (Object[] r : rows) {
            ProductLocationResponse dto = ProductLocationResponse.builder()
                    .productId(((Number) r[0]).longValue())
                    .productName((String) r[1])
                    .price((BigDecimal) r[2])
                    .storeId(r[3] != null ? ((Number) r[3]).longValue() : null)
                    .storeName((String) r[4])
                    .storeLatitude(r[5] != null ? ((Number) r[5]).doubleValue() : null)
                    .storeLongitude(r[6] != null ? ((Number) r[6]).doubleValue() : null)
                    .distanceKm(r[7] != null ? ((Number) r[7]).doubleValue() : null)
                    .build();
            results.add(dto);
        }

        return new PageImpl<>(results, PageRequest.of(page, size), results.size());
    }

    @Override
    public Page<ProductLocationResponse> searchProductsByStore(Long storeId, int page, int size) {
        com.eshop.app.dto.request.ProductLocationSearchRequest req = com.eshop.app.dto.request.ProductLocationSearchRequest
                .builder()
                .storeId(storeId)
                .page(page)
                .size(size)
                .build();
        return searchProductsByLocation(req);
    }

    @Override
    public Double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        double R = 6371.0; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Override
    public Double convertKmToMiles(Double km) {
        return km * 0.621371;
    }

    @Override
    public Double[] getUserLocationFromIp(String ipAddress) {
        // Simple stub: real implementation would call Google Geolocation API.
        // Return nulls to indicate unknown location.
        return new Double[] { null, null };
    }
}
