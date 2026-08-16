package com.eshop.app.pricing.application.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.eshop.app.catalog.domain.repository.CategoryRepository;
import com.eshop.app.order.domain.repository.OrderRepository;
import com.eshop.app.pricing.api.response.CouponResponse;
import com.eshop.app.pricing.domain.entity.Coupon;
import com.eshop.app.pricing.domain.repository.CouponRepository;
import com.eshop.app.pricing.domain.repository.CouponUsageRepository;
import com.eshop.app.store.domain.repository.StoreRepository;
import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.domain.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Verifies that {@link Coupon#canBeUsedByUser} — previously unreachable dead logic — is now
 * actually enforced by {@link CouponUseCaseImpl#validateCoupon}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CouponUseCaseImplTest {

    @Mock CouponRepository couponRepository;
    @Mock CouponUsageRepository couponUsageRepository;
    @Mock StoreRepository storeRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock UserRepository userRepository;
    @Mock OrderRepository orderRepository;

    private CouponUseCaseImpl couponUseCase;

    private static final Long USER_ID = 42L;
    private static final Long COUPON_ID = 7L;

    @BeforeEach
    void setUp() {
        couponUseCase = new CouponUseCaseImpl(
                couponRepository, couponUsageRepository, storeRepository, categoryRepository,
                userRepository, orderRepository);
    }

    @Test
    void validateCoupon_rejectsWhenUserExceededPerUserLimit() {
        Coupon coupon = activeCoupon();
        coupon.setUsageLimitPerUser(1);
        when(couponRepository.findActiveByCode(eq("SAVE10"), any())).thenReturn(Optional.of(coupon));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser()));
        when(couponUsageRepository.countByCouponIdAndUserId(COUPON_ID, USER_ID)).thenReturn(1);

        CouponResponse.ValidationResult result =
                couponUseCase.validateCoupon("SAVE10", USER_ID, new BigDecimal("100.00"), null, null);

        assertFalse(result.getIsValid());
        assertEquals("USER_USAGE_LIMIT_EXCEEDED", result.getErrorCode());
        assertEquals(BigDecimal.ZERO, result.getDiscountAmount());
    }

    @Test
    void validateCoupon_acceptsWhenUserUnderPerUserLimit() {
        Coupon coupon = activeCoupon();
        coupon.setUsageLimitPerUser(3);
        when(couponRepository.findActiveByCode(eq("SAVE10"), any())).thenReturn(Optional.of(coupon));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser()));
        when(couponUsageRepository.countByCouponIdAndUserId(COUPON_ID, USER_ID)).thenReturn(1);

        CouponResponse.ValidationResult result =
                couponUseCase.validateCoupon("SAVE10", USER_ID, new BigDecimal("100.00"), null, null);

        assertEquals(true, result.getIsValid());
        assertEquals(0, new BigDecimal("10.00").compareTo(result.getDiscountAmount()));
    }

    @Test
    void validateCoupon_rejectsWhenGlobalUsageLimitReached() {
        Coupon coupon = activeCoupon();
        coupon.setUsageLimit(5);
        coupon.setUsedCount(5);
        when(couponRepository.findActiveByCode(eq("SAVE10"), any())).thenReturn(Optional.of(coupon));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser()));
        when(couponUsageRepository.countByCouponIdAndUserId(anyLong(), anyLong())).thenReturn(0);

        CouponResponse.ValidationResult result =
                couponUseCase.validateCoupon("SAVE10", USER_ID, new BigDecimal("100.00"), null, null);

        assertFalse(result.getIsValid());
    }

    private Coupon activeCoupon() {
        Coupon coupon = Coupon.builder()
                .code("SAVE10")
                .name("Save 10%")
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10"))
                .isActive(true)
                .usedCount(0)
                .firstTimeOnly(false)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(30))
                .appliesTo(Coupon.AppliesTo.ALL_ORDERS)
                .build();
        coupon.setId(COUPON_ID);
        return coupon;
    }

    private User existingUser() {
        User user = User.builder().build();
        user.setId(USER_ID);
        user.setCreatedAt(LocalDateTime.now().minusYears(1));
        return user;
    }
}
