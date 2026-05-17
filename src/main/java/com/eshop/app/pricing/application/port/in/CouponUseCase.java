package com.eshop.app.pricing.application.port.in;

import com.eshop.app.pricing.api.request.CouponRequest;
import com.eshop.app.pricing.api.request.CouponUsageRequest;
import com.eshop.app.pricing.api.response.CouponResponse;
import com.eshop.app.core.api.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Inbound Port for Coupon Use Cases.
 */
public interface CouponUseCase {

    CouponResponse createCoupon(CouponRequest request);

    CouponResponse updateCoupon(Long couponId, CouponRequest request);

    CouponResponse getCouponById(Long couponId);

    CouponResponse getCouponByCode(String code);

    PageResponse<CouponResponse> getAllCoupons(Pageable pageable);

    PageResponse<CouponResponse> getActiveCoupons(Pageable pageable);

    PageResponse<CouponResponse> getCouponsByStore(Long storeId, Pageable pageable);

    PageResponse<CouponResponse> getCouponsByCategory(Long categoryId, Pageable pageable);

    CouponResponse.ValidationResult validateCoupon(String code, Long userId,
            BigDecimal orderTotal,
            Long storeId, Long categoryId);

    CouponResponse.ApplicationResult applyCoupon(CouponUsageRequest request);

    List<CouponResponse> getApplicableCoupons(Long userId, BigDecimal orderTotal,
            Long storeId, Long categoryId);

    List<CouponResponse> getGlobalActiveCoupons();

    PageResponse<CouponResponse> searchCoupons(String keyword, Pageable pageable);

    void deleteCoupon(Long couponId);

    Object getCouponStatistics();

    List<CouponResponse> getCouponsExpiringSoon(int days);

    int deactivateExpiredCoupons();

    PageResponse<Object> getUserCouponUsageHistory(Long userId, Pageable pageable);

    String generateCouponCode(String prefix);
}

