package com.eshop.app.pricing.application.port.in;

import com.eshop.app.catalog.domain.entity.CategoryCommission;
import com.eshop.app.order.domain.entity.Order;
import com.eshop.app.seller.domain.entity.SellerWallet;
import java.math.BigDecimal;

/**
 * Inbound Port for Commission Use Cases.
 */
public interface CommissionUseCase {
    CategoryCommission setCategoryCommission(Long categoryId, BigDecimal percentage, BigDecimal flatFee);
    BigDecimal calculateCommission(BigDecimal price, Long categoryId);
    void processOrderCommission(Order order);
    SellerWallet getSellerWallet(Long sellerId);
}
