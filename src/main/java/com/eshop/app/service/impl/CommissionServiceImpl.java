package com.eshop.app.service.impl;

import com.eshop.app.config.properties.AppProperties;
import com.eshop.app.entity.*;
import com.eshop.app.repository.CategoryCommissionRepository;
import com.eshop.app.repository.SellerWalletRepository;
import com.eshop.app.service.CommissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class CommissionServiceImpl implements CommissionService {

    private final CategoryCommissionRepository commissionRepository;
    private final SellerWalletRepository sellerWalletRepository;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public CategoryCommission setCategoryCommission(Long categoryId, BigDecimal percentage, BigDecimal flatFee) {
        Category categoryProxy = new Category();
        categoryProxy.setId(categoryId);

        CategoryCommission commission = commissionRepository.findByCategoryId(categoryId)
                .orElse(CategoryCommission.builder()
                        .category(categoryProxy)
                        .build());

        commission.setCommissionPercentage(percentage);
        commission.setFlatFee(flatFee);
        return commissionRepository.save(commission);
    }

    @Override
    public BigDecimal calculateCommission(BigDecimal price, Long categoryId) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        CategoryCommission rule = commissionRepository.findByCategoryIdAndActiveTrue(categoryId)
                .orElse(null);

        if (rule == null) {
            // Use dynamic default from AppProperties
            BigDecimal defaultRate = BigDecimal.valueOf(appProperties.getBusiness().getDefaultCommissionRate());
            return price.multiply(defaultRate);
        }

        BigDecimal percentageFee = price.multiply(rule.getCommissionPercentage())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        return percentageFee.add(rule.getFlatFee());
    }

    @Override
    @Transactional
    public void processOrderCommission(Order order) {
        // Implementation logic
    }

    @Override
    public SellerWallet getSellerWallet(Long sellerId) {
        return sellerWalletRepository.findByUserId(sellerId)
                .orElseGet(() -> {
                    User userProxy = new User();
                    userProxy.setId(sellerId);

                    return sellerWalletRepository.save(SellerWallet.builder()
                            .user(userProxy)
                            .currentBalance(BigDecimal.ZERO)
                            .withdrawableBalance(BigDecimal.ZERO)
                            .currency(appProperties.getBusiness().getDefaultCurrency())
                            .build());
                });
    }
}
