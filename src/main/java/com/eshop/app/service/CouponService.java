package com.eshop.app.service;

import com.eshop.app.dto.request.CouponRequest;
import com.eshop.app.dto.request.CouponUsageRequest;
import com.eshop.app.dto.response.CouponResponse;
import com.eshop.app.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Coupon service interface for discount and promotional management
 * Handles coupon creation, validation, and application logic
 * 
 * Time Complexity: All operations O(1) or O(log n)
 * Space Complexity: O(1) for processing, O(n) for batch operations
 */
public interface CouponService {
    
    /**
     * Create new coupon
     * 
     * @param request Coupon details including code, discount, validity
     * @return Created coupon response
     * 
     * Business Rules:
     * - Coupon codes must be unique
     * - Valid date range required
     * - Discount value validation based on type
     */
    CouponResponse createCoupon(CouponRequest request);
    
    /**
     * Update existing coupon
     */
    CouponResponse updateCoupon(Long couponId, CouponRequest request);
    
    /**
     * Get coupon by ID
     */
    CouponResponse getCouponById(Long couponId);
    
    /**
     * Get coupon by code
     */
    CouponResponse getCouponByCode(String code);
    
    /**
     * Get all coupons with pagination
     */
    PageResponse<CouponResponse> getAllCoupons(Pageable pageable);
    
    /**
     * Get active coupons
     */
    PageResponse<CouponResponse> getActiveCoupons(Pageable pageable);
    
    /**
     * Get coupons by store
     */
    PageResponse<CouponResponse> getCouponsByStore(Long storeId, Pageable pageable);
    
    /**
     * Get coupons by category
     */
    PageResponse<CouponResponse> getCouponsByCategory(Long categoryId, Pageable pageable);
    
    /**
     * Validate coupon for user and order
     * 
     * @param code Coupon code to validate
     * @param userId User attempting to use coupon
     * @param orderTotal Order total amount
     * @param shopId Optional shop ID for shop-specific coupons
     * @param categoryId Optional category ID for category-specific coupons
     * @return Validation result with discount calculation
     * 
     * Business Rules:
     * - Checks coupon validity (active, date range, usage limits)
     * - Validates user eligibility (first-time only, usage per user)
     * - Calculates applicable discount amount
     * - Checks minimum order requirements
     */
    CouponResponse.ValidationResult validateCoupon(String code, Long userId, 
                                                  BigDecimal orderTotal, 
                                                  Long storeId, Long categoryId);
    
    /**
     * Apply coupon to order (use coupon)
     * 
     * @param request Coupon usage details
     * @return Applied coupon details with final discount
     * 
     * Business Rules:
     * - Validates coupon before application
     * - Increments usage count
     * - Deactivates if usage limit reached
     * - Records usage history
     */
    CouponResponse.ApplicationResult applyCoupon(CouponUsageRequest request);
    
    /**
     * Get applicable coupons for user and order
     * Returns all valid coupons that user can apply to current order
     */
    List<CouponResponse> getApplicableCoupons(Long userId, BigDecimal orderTotal, 
                                            Long storeId, Long categoryId);
    
    /**
     * Get global active coupons (not store/category specific)
     */
    List<CouponResponse> getGlobalActiveCoupons();
    
    /**
     * Search coupons by code or name
     */
    PageResponse<CouponResponse> searchCoupons(String keyword, Pageable pageable);
    
    /**
     * Delete coupon (soft delete - mark as inactive)
     */
    void deleteCoupon(Long couponId);
    
    /**
     * Get coupon usage statistics
     */
    Object getCouponStatistics();
    
    /**
     * Get coupons expiring soon (for notifications)
     */
    List<CouponResponse> getCouponsExpiringSoon(int days);
    
    /**
     * Deactivate expired coupons (cleanup job)
     */
    int deactivateExpiredCoupons();
    
    /**
     * Get user's coupon usage history
     */
    PageResponse<Object> getUserCouponUsageHistory(Long userId, Pageable pageable);
    
    /**
     * Generate unique coupon code
     */
    String generateCouponCode(String prefix);
}