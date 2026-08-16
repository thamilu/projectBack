package com.eshop.app.catalog.application.service.impl;

import com.eshop.app.catalog.api.request.ProductUpdateRequest;
import com.eshop.app.catalog.api.response.ProductResponse;
import com.eshop.app.catalog.application.mapper.ProductMapper;
import com.eshop.app.catalog.application.service.ProductServiceHelper;
import com.eshop.app.catalog.domain.entity.Brand;
import com.eshop.app.catalog.domain.entity.Category;
import com.eshop.app.catalog.domain.entity.Product;
import com.eshop.app.catalog.domain.repository.BrandRepository;
import com.eshop.app.catalog.domain.repository.CategoryRepository;
import com.eshop.app.catalog.domain.repository.ProductRepository;
import com.eshop.app.catalog.infrastructure.config.ProductProperties;
import com.eshop.app.core.events.domain.LowStockEvent;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import static com.eshop.app.core.infrastructure.config.security.SecurityExpressions.*;
import com.eshop.app.inventory.api.request.StockUpdateRequest;
import com.eshop.app.inventory.shared.exception.InsufficientStockException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class ProductUpdateService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductMapper productMapper;
    private final ProductServiceHelper helper;
    private final ApplicationEventPublisher eventPublisher;
    private final ProductProperties productProperties;

    @Transactional
    @PreAuthorize(IS_ADMIN_OR_SELLER)
    public ProductResponse updateProduct(
            @NotNull @Positive Long id, @Valid @NotNull ProductUpdateRequest request) {
        Product product =
                productRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Product not found with id: " + id));

        Category category =
                categoryRepository
                        .findById(request.getCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        product.setName(request.getName());
        product.setDescription(helper.sanitize(request.getDescription()));

        if (request.getFriendlyUrl() != null && !request.getFriendlyUrl().trim().isEmpty()) {
            String newFriendlyUrl = request.getFriendlyUrl();
            if (!newFriendlyUrl.equals(product.getFriendlyUrl())) {
                newFriendlyUrl = helper.ensureUniqueFriendlyUrl(newFriendlyUrl);
                product.setFriendlyUrl(newFriendlyUrl);
            }
        } else if (!request.getName().equals(product.getName())) {
            String newFriendlyUrl = helper.generateFriendlyUrl(request.getName());
            newFriendlyUrl = helper.ensureUniqueFriendlyUrl(newFriendlyUrl);
            product.setFriendlyUrl(newFriendlyUrl);
        }

        product.setPrice(request.getPrice());
        product.setDiscountPrice(request.getDiscountPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);

        if (request.getBrandId() != null) {
            Brand brand =
                    brandRepository
                            .findById(request.getBrandId())
                            .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));
            product.setBrand(brand);
        } else {
            product.setBrand(null);
        }

        if (request.getTags() != null) {
            product.setTags(helper.resolveOrCreateTags(request.getTags()));
        }

        if (request.getFeatured() != null) {
            product.setFeatured(request.getFeatured());
        }

        if (request.getActive() != null) {
            if (Boolean.TRUE.equals(request.getActive())) {
                product.activate();
            } else {
                product.deactivate();
            }
        }

        product = productRepository.save(product);
        return productMapper.toProductResponse(product);
    }

    @Transactional
    @PreAuthorize(IS_ADMIN_OR_SELLER)
    public ProductResponse updateStockAndReturn(Long id, StockUpdateRequest request) {
        Product product =
                productRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Product not found with id: " + id));

        int oldStock = product.getStockQuantity();
        int newStock =
                switch (request.operation()) {
                    case SET -> request.quantity();
                    case INCREMENT -> oldStock + request.quantity();
                    case DECREMENT -> oldStock - request.quantity();
                };

        if (newStock < 0) {
            throw new InsufficientStockException(
                    String.format(
                            "Insufficient stock for product %s: requested %d, available %d",
                            product.getName(), request.quantity(), oldStock));
        }

        product.setStockQuantity(newStock);
        Product saved = productRepository.save(product);

        if (newStock < productProperties.getLowStockThreshold()) {
            eventPublisher.publishEvent(new LowStockEvent(this, saved));
        }

        return productMapper.toProductResponse(saved);
    }

    @Transactional
    @PreAuthorize(IS_ADMIN_OR_SELLER)
    public void updateStock(Long productId, Integer quantity) {
        changeStockQuantity(productId, quantity);
    }

    @Transactional
    @PreAuthorize(IS_ADMIN_OR_SELLER)
    public void adjustStock(Long productId, int delta) {
        changeStockQuantity(productId, delta);
    }

    private void changeStockQuantity(Long productId, int delta) {
        Product product =
                productRepository
                        .findByIdForUpdate(productId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Product not found with id: " + productId));
        int newStock = product.getStockQuantity() + delta;
        if (newStock < 0) {
            throw new InsufficientStockException(
                    "Insufficient stock for product: " + product.getName());
        }
        product.setStockQuantity(newStock);
        productRepository.save(product);
    }
}
