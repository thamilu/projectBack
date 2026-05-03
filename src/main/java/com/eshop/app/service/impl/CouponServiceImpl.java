package com.eshop.app.service.impl;

import com.eshop.app.dto.request.CouponRequest;
import com.eshop.app.dto.request.CouponUsageRequest;
import com.eshop.app.dto.response.CouponResponse;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.entity.Category;
import com.eshop.app.entity.Coupon;
import com.eshop.app.entity.Store;
import com.eshop.app.exception.ResourceNotFoundException;
import com.eshop.app.repository.CategoryRepository;
import com.eshop.app.repository.CouponRepository;
import com.eshop.app.repository.StoreRepository;
import com.eshop.app.service.CouponService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Transactional
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;

    public CouponServiceImpl(CouponRepository couponRepository,
            StoreRepository storeRepository,
            CategoryRepository categoryRepository) {
        this.couponRepository = couponRepository;
        this.storeRepository = storeRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public CouponResponse createCoupon(CouponRequest request) {
        Coupon coupon = mapToEntity(new Coupon(), request);
        Coupon saved = couponRepository.save(coupon);
        return mapToResponse(saved);
    }

    @Override
    public CouponResponse updateCoupon(Long couponId, CouponRequest request) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + couponId));
        coupon = mapToEntity(coupon, request);
        Coupon saved = couponRepository.save(coupon);
        return mapToResponse(saved);
    }

    @Override
    public CouponResponse getCouponById(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + couponId));
        return mapToResponse(coupon);
    }

    @Override
    public CouponResponse getCouponByCode(String code) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with code: " + code));
        return mapToResponse(coupon);
    }

    @Override
    public PageResponse<CouponResponse> getAllCoupons(Pageable pageable) {
        Page<Coupon> page = couponRepository.findAll(pageable);
        return toPageResponse(page);
    }

    @Override
    public PageResponse<CouponResponse> getActiveCoupons(Pageable pageable) {
        Page<Coupon> page = couponRepository.findByIsActiveTrue(pageable);
        return toPageResponse(page);
    }

    @Override
    public PageResponse<CouponResponse> getCouponsByStore(Long storeId, Pageable pageable) {
        Page<Coupon> page = couponRepository.findByStoreId(storeId, pageable);
        return toPageResponse(page);
    }

    @Override
    public PageResponse<CouponResponse> getCouponsByCategory(Long categoryId, Pageable pageable) {
        Page<Coupon> page = couponRepository.findByCategoryId(categoryId, pageable);
        return toPageResponse(page);
    }

    @Override
    public CouponResponse.ValidationResult validateCoupon(String code, Long userId, BigDecimal orderTotal, Long storeId,
            Long categoryId) {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = couponRepository.findActiveByCode(code, now)
                .orElseThrow(() -> new ResourceNotFoundException("Active coupon not found with code: " + code));

        if (coupon.getMinimumOrderAmount() != null && orderTotal.compareTo(coupon.getMinimumOrderAmount()) < 0) {
            return CouponResponse.ValidationResult.builder()
                    .isValid(false)
                    .message("Order total is below minimum amount required")
                    .errorCode("MIN_ORDER_NOT_MET")
                    .discountAmount(BigDecimal.ZERO)
                    .coupon(mapToResponse(coupon))
                    .build();
        }

        BigDecimal discount = coupon.calculateDiscount(orderTotal);

        return CouponResponse.ValidationResult.builder()
                .isValid(discount.compareTo(BigDecimal.ZERO) > 0)
                .message("Coupon validated")
                .discountAmount(discount)
                .errorCode(null)
                .coupon(mapToResponse(coupon))
                .build();
    }

    @Override
    public CouponResponse.ApplicationResult applyCoupon(CouponUsageRequest request) {
        CouponResponse.ValidationResult validation = validateCoupon(
                request.getCouponCode(),
                request.getUserId(),
                request.getOrderTotal(),

                request.getStoreId(),
                request.getCategoryId());

        if (!Boolean.TRUE.equals(validation.getIsValid())) {
            return CouponResponse.ApplicationResult.builder()
                    .success(false)
                    .message(validation.getMessage())
                    .appliedDiscount(BigDecimal.ZERO)
                    .finalTotal(request.getOrderTotal())
                    .couponCode(request.getCouponCode())
                    .coupon(validation.getCoupon())
                    .build();
        }

        Coupon coupon = couponRepository.findByCode(request.getCouponCode())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Coupon not found with code: " + request.getCouponCode()));

        coupon.use();
        couponRepository.save(coupon);

        BigDecimal finalTotal = request.getOrderTotal().subtract(validation.getDiscountAmount());

        return CouponResponse.ApplicationResult.builder()
                .success(true)
                .message("Coupon applied successfully")
                .appliedDiscount(validation.getDiscountAmount())
                .finalTotal(finalTotal)
                .couponCode(request.getCouponCode())
                .coupon(mapToResponse(coupon))
                .build();
    }

    @Override
    public List<CouponResponse> getApplicableCoupons(Long userId, BigDecimal orderTotal, Long storeId, Long categoryId) {
        LocalDateTime now = LocalDateTime.now();
        return couponRepository.findGlobalActiveCoupons(now).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CouponResponse> getGlobalActiveCoupons() {
        LocalDateTime now = LocalDateTime.now();
        return couponRepository.findGlobalActiveCoupons(now).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<CouponResponse> searchCoupons(String keyword, Pageable pageable) {
        Page<Coupon> page = couponRepository.searchCoupons(keyword, pageable);
        return toPageResponse(page);
    }

    @Override
    public void deleteCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + couponId));
        coupon.setIsActive(false);
        couponRepository.save(coupon);
    }

    @Override
    public Object getCouponStatistics() {
        return couponRepository.getCouponUsageStatistics();
    }

    @Override
    public List<CouponResponse> getCouponsExpiringSoon(int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime soon = now.plusDays(days);
        return couponRepository.findCouponsExpiringSoon(now, soon).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public int deactivateExpiredCoupons() {
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> expired = couponRepository.findExpiredActiveCoupons(now);
        expired.forEach(c -> c.setIsActive(false));
        couponRepository.saveAll(expired);
        return expired.size();
    }

    @Override
    public PageResponse<Object> getUserCouponUsageHistory(Long userId, Pageable pageable) {
        // Placeholder implementation: no history tracking yet
        PageResponse.PageMetadata metadata = PageResponse.PageMetadata.builder()
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(0L)
                .totalPages(0)
                .hasNext(false)
                .hasPrevious(false)
                .build();
        return PageResponse.<Object>builder()
                .data(List.of())
                .pagination(metadata)
                .build();
    }

    @Override
    public String generateCouponCode(String prefix) {
        String base = (prefix != null ? prefix.toUpperCase() : "COUPON");
        String random = Integer.toHexString(new Random().nextInt(0xFFFFF)).toUpperCase();
        return base + "-" + random;
    }

    private Coupon mapToEntity(Coupon coupon, CouponRequest request) {
        coupon.setCode(request.getCode());
        coupon.setName(request.getName());
        coupon.setDescription(request.getDescription());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinimumOrderAmount(request.getMinimumOrderAmount());
        coupon.setMaximumDiscountAmount(request.getMaximumDiscountAmount());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setUsageLimitPerUser(request.getUsageLimitPerUser());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidUntil(request.getValidUntil());
        coupon.setAppliesTo(request.getAppliesTo());
        coupon.setFirstTimeOnly(request.getFirstTimeOnly());

        if (request.getStoreId() != null) {
            Store store = storeRepository.findById(request.getStoreId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Store not found with id: " + request.getStoreId()));
            coupon.setStore(store);
        } else {
            coupon.setStore(null);
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found with id: " + request.getCategoryId()));
            coupon.setCategory(category);
        } else {
            coupon.setCategory(null);
        }

        if (coupon.getIsActive() == null) {
            coupon.setIsActive(true);
        }
        if (coupon.getUsedCount() == null) {
            coupon.setUsedCount(0);
        }

        return coupon;
    }

    private CouponResponse mapToResponse(Coupon coupon) {
        Long storeId = coupon.getStore() != null ? coupon.getStore().getId() : null;
        String storeName = coupon.getStore() != null ? coupon.getStore().getStoreName() : null;
        Long categoryId = coupon.getCategory() != null ? coupon.getCategory().getId() : null;
        String categoryName = coupon.getCategory() != null ? coupon.getCategory().getName() : null;

        Integer remainingUses = null;
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() != null) {
            remainingUses = Math.max(0, coupon.getUsageLimit() - coupon.getUsedCount());
        }

        boolean isExpiringSoon = coupon.getValidUntil() != null &&
                coupon.getValidUntil().isBefore(LocalDateTime.now().plusDays(7));

        String discountDisplay;
        if (coupon.getDiscountType() == Coupon.DiscountType.PERCENTAGE) {
            discountDisplay = coupon.getDiscountValue().stripTrailingZeros().toPlainString() + "% OFF";
        } else {
            discountDisplay = coupon.getDiscountValue().stripTrailingZeros().toPlainString() + " OFF";
        }

        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .name(coupon.getName())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minimumOrderAmount(coupon.getMinimumOrderAmount())
                .maximumDiscountAmount(coupon.getMaximumDiscountAmount())
                .usageLimit(coupon.getUsageLimit())
                .usageLimitPerUser(coupon.getUsageLimitPerUser())
                .usedCount(coupon.getUsedCount())
                .validFrom(coupon.getValidFrom())
                .validUntil(coupon.getValidUntil())
                .isActive(coupon.getIsActive())
                .appliesTo(coupon.getAppliesTo())
                .storeId(storeId)
                .storeName(storeName)
                .categoryId(categoryId)
                .categoryName(categoryName)
                .firstTimeOnly(coupon.getFirstTimeOnly())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .remainingUses(remainingUses)
                .isExpiringSoon(isExpiringSoon)
                .discountDisplay(discountDisplay)
                .build();
    }

    private PageResponse<CouponResponse> toPageResponse(Page<Coupon> page) {
        List<CouponResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        PageResponse.PageMetadata metadata = PageResponse.PageMetadata.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
        return PageResponse.<CouponResponse>builder()
                .data(content)
                .pagination(metadata)
                .build();
    }
}
