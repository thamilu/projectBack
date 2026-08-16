package com.eshop.app.pricing.domain.repository;

import com.eshop.app.pricing.domain.entity.CouponUsage;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    /** Number of times a given user has used a given coupon — drives {@code usageLimitPerUser}. */
    int countByCouponIdAndUserId(@Param("couponId") Long couponId, @Param("userId") Long userId);

    Page<CouponUsage> findByUserIdOrderByUsedAtDesc(@Param("userId") Long userId, Pageable pageable);
}
