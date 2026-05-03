package com.eshop.app.controller;

import com.eshop.app.dto.request.CouponRequest;
import com.eshop.app.dto.request.CouponUsageRequest;
import com.eshop.app.dto.response.CouponResponse;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.eshop.app.constants.ApiConstants;

import java.math.BigDecimal;
import java.util.List;

/**
 * Coupon Controller for discount and promotional management
 * Provides comprehensive coupon creation, validation, and application
 * 
 * Security: Role-based access with coupon-specific permissions
 * Performance: O(1) validation with efficient caching
 */
@Tag(name = "Coupon Management", description = "Discount coupons, validation, and promotional management")
@RestController
@RequestMapping(ApiConstants.Endpoints.COUPONS)
@RequiredArgsConstructor
@SecurityRequirement(name = "Keycloak OAuth2")
@SecurityRequirement(name = "Bearer Authentication")
public class CouponController {
    
    private final CouponService couponService;
    
    @PostMapping
    @Operation(summary = "Create Coupon", 
               description = "Create a new discount coupon with validation rules")
    @ApiResponse(responseCode = "201", description = "Coupon created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid coupon data")
    @ApiResponse(responseCode = "409", description = "Coupon code already exists")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CouponRequest request) {
        CouponResponse response = couponService.createCoupon(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @PutMapping("/{couponId}")
    @Operation(summary = "Update Coupon", 
               description = "Update existing coupon details")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<CouponResponse> updateCoupon(
            @Parameter(description = "Coupon ID") @PathVariable Long couponId,
            @Valid @RequestBody CouponRequest request) {
        CouponResponse response = couponService.updateCoupon(couponId, request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{couponId}")
    @Operation(summary = "Get Coupon by ID", 
               description = "Retrieve coupon details by ID")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CouponResponse> getCouponById(
            @Parameter(description = "Coupon ID") @PathVariable Long couponId) {
        CouponResponse response = couponService.getCouponById(couponId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/code/{code}")
    @Operation(summary = "Get Coupon by Code", 
               description = "Retrieve coupon details by coupon code")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CouponResponse> getCouponByCode(
            @Parameter(description = "Coupon code") @PathVariable String code) {
        CouponResponse response = couponService.getCouponByCode(code);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @Operation(summary = "Get All Coupons", 
               description = "Retrieve paginated list of all coupons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<CouponResponse>> getAllCoupons(
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<CouponResponse> coupons = couponService.getAllCoupons(pageable);
        return ResponseEntity.ok(coupons);
    }
    
    @GetMapping("/active")
    @Operation(summary = "Get Active Coupons", 
               description = "Retrieve paginated list of currently active coupons")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<PageResponse<CouponResponse>> getActiveCoupons(
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<CouponResponse> coupons = couponService.getActiveCoupons(pageable);
        return ResponseEntity.ok(coupons);
    }
    
    @GetMapping("/store/{storeId}")
    @Operation(summary = "Get Store Coupons", 
               description = "Retrieve coupons for a specific store")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<PageResponse<CouponResponse>> getCouponsByStore(
            @Parameter(description = "Store ID") @PathVariable Long storeId,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<CouponResponse> coupons = couponService.getCouponsByStore(storeId, pageable);
        return ResponseEntity.ok(coupons);
    }
    
    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get Category Coupons", 
               description = "Retrieve coupons for a specific category")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<PageResponse<CouponResponse>> getCouponsByCategory(
            @Parameter(description = "Category ID") @PathVariable Long categoryId,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<CouponResponse> coupons = couponService.getCouponsByCategory(categoryId, pageable);
        return ResponseEntity.ok(coupons);
    }
    
    @PostMapping("/validate")
    @Operation(summary = "Validate Coupon", 
               description = "Validate coupon for specific order and calculate discount")
    @ApiResponse(responseCode = "200", description = "Coupon validation result")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<CouponResponse.ValidationResult> validateCoupon(
            @Parameter(description = "Coupon code") @RequestParam String code,
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Order total") @RequestParam BigDecimal orderTotal,
            @Parameter(description = "Store ID (optional)") @RequestParam(required = false) Long storeId,
            @Parameter(description = "Category ID (optional)") @RequestParam(required = false) Long categoryId) {
        CouponResponse.ValidationResult result = couponService.validateCoupon(code, userId, orderTotal, storeId, categoryId);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/apply")
    @Operation(summary = "Apply Coupon", 
               description = "Apply coupon to order and get final discount")
    @ApiResponse(responseCode = "200", description = "Coupon applied successfully")
    @ApiResponse(responseCode = "400", description = "Invalid coupon or cannot be applied")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<CouponResponse.ApplicationResult> applyCoupon(
            @Valid @RequestBody CouponUsageRequest request) {
        CouponResponse.ApplicationResult result = couponService.applyCoupon(request);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/applicable")
    @Operation(summary = "Get Applicable Coupons", 
               description = "Get all coupons that can be applied to current order")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<CouponResponse>> getApplicableCoupons(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Order total") @RequestParam BigDecimal orderTotal,
            @Parameter(description = "Store ID (optional)") @RequestParam(required = false) Long storeId,
            @Parameter(description = "Category ID (optional)") @RequestParam(required = false) Long categoryId) {
        List<CouponResponse> coupons = couponService.getApplicableCoupons(userId, orderTotal, storeId, categoryId);
        return ResponseEntity.ok(coupons);
    }
    
    @GetMapping("/global/active")
    @Operation(summary = "Get Global Active Coupons", 
               description = "Get global coupons that are currently active")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<CouponResponse>> getGlobalActiveCoupons() {
        List<CouponResponse> coupons = couponService.getGlobalActiveCoupons();
        return ResponseEntity.ok(coupons);
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search Coupons", 
               description = "Search coupons by code or name")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<CouponResponse>> searchCoupons(
            @Parameter(description = "Search keyword") @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<CouponResponse> coupons = couponService.searchCoupons(keyword, pageable);
        return ResponseEntity.ok(coupons);
    }
    
    @DeleteMapping("/{couponId}")
    @Operation(summary = "Delete Coupon", 
               description = "Soft delete coupon (mark as inactive)")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCoupon(
            @Parameter(description = "Coupon ID") @PathVariable Long couponId) {
        couponService.deleteCoupon(couponId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "Get Coupon Statistics", 
               description = "Get coupon usage statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> getCouponStatistics() {
        Object statistics = couponService.getCouponStatistics();
        return ResponseEntity.ok(statistics);
    }
    
    @GetMapping("/expiring")
    @Operation(summary = "Get Expiring Coupons", 
               description = "Get coupons expiring in specified days")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CouponResponse>> getCouponsExpiringSoon(
            @Parameter(description = "Days until expiration") @RequestParam(defaultValue = "7") int days) {
        List<CouponResponse> coupons = couponService.getCouponsExpiringSoon(days);
        return ResponseEntity.ok(coupons);
    }
    
    @PostMapping("/generate-code")
    @Operation(summary = "Generate Coupon Code", 
               description = "Generate unique coupon code with prefix")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<String> generateCouponCode(
            @Parameter(description = "Code prefix") @RequestParam(required = false) String prefix) {
        String code = couponService.generateCouponCode(prefix);
        return ResponseEntity.ok(code);
    }
}