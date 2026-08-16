package com.eshop.app.catalog.application.service.impl;

import com.eshop.app.catalog.domain.repository.ProductRepository;
import com.eshop.app.catalog.shared.exception.ProductDeletionException;
import com.eshop.app.catalog.shared.exception.ProductNotFoundException;
import com.eshop.app.core.api.response.BatchOperationResult;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import static com.eshop.app.core.infrastructure.config.security.SecurityExpressions.*;
import com.eshop.app.order.domain.entity.OrderItem;
import com.eshop.app.order.domain.repository.OrderItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductDeleteService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    @PreAuthorize(IS_ADMIN_OR_SELLER)
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }

        List<OrderItem> activeOrders = orderItemRepository.findByProductId(id);
        if (!activeOrders.isEmpty()) {
            throw new ProductDeletionException(
                    id, "Product has " + activeOrders.size() + " active order items");
        }

        productRepository.deleteById(id);
    }

    @Transactional
    @PreAuthorize(IS_ADMIN)
    public BatchOperationResult<Long> deleteProductsBatch(List<Long> ids, String userId) {
        log.info("Starting batch product deletion: {} items by user {}", ids.size(), userId);
        BatchOperationResult.Builder<Long> resultBuilder = BatchOperationResult.builder();
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            try {
                deleteProduct(id);
                resultBuilder.addSuccess(id);
            } catch (ResourceNotFoundException e) {
                resultBuilder.addFailure(i, String.valueOf(id), e.getMessage(), "NOT_FOUND");
            } catch (Exception e) {
                resultBuilder.addFailure(i, String.valueOf(id), e.getMessage(), "INTERNAL_ERROR");
            }
        }
        return resultBuilder.build();
    }
}
