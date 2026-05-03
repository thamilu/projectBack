package com.eshop.app.service;

import com.eshop.app.entity.CategoryCommission;
import com.eshop.app.entity.Order;
import com.eshop.app.entity.SellerWallet;

import java.math.BigDecimal;

public interface CommissionService {

    /**
     * Set commission rule for a category.
     */
    CategoryCommission setCategoryCommission(Long categoryId, BigDecimal percentage, BigDecimal flatFee);

    /**
     * Calculate commission for a specific item price and category.
     */
    BigDecimal calculateCommission(BigDecimal price, Long categoryId);

    /**
     * Process order commission and update seller wallet.
     * Should be called when order is completed.
     */
    void processOrderCommission(Order order);

    /**
     * Get or create seller wallet.
     */
    SellerWallet getSellerWallet(Long sellerId);
}
