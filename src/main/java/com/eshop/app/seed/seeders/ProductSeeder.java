package com.eshop.app.seed.seeders;


import com.eshop.app.entity.*;
import com.eshop.app.entity.enums.ProductStatus;
import com.eshop.app.repository.ProductRepository;
import com.eshop.app.seed.core.Seeder;
import com.eshop.app.seed.core.SeederContext;
import com.eshop.app.seed.exception.CatalogSeedingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Product seeder - Order 6.
 * Creates products with all relationships.
 * Depends on Category, Brand, Store, and Tag seeders.
 */
@Slf4j
@Component
@Order(6)
@RequiredArgsConstructor
public class ProductSeeder implements Seeder<Product, SeederContext> {

    private final ProductRepository productRepository;
    private final com.eshop.app.seed.provider.ProductDataProvider productDataProvider;

    @Override
    public List<Product> seed(SeederContext context) {
        try {
            List<Product> products = productDataProvider.getProducts().stream()
                    .map(cfg -> buildProduct(cfg, context))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();

            List<Product> savedProducts = productRepository.saveAll(products);

            log.info("Seeded {} products successfully", savedProducts.size());
            return savedProducts;

        } catch (DataAccessException e) {
            throw new CatalogSeedingException(
                    "Failed to seed products: " + e.getMessage(), e);
        }
    }

    @Override
    public void cleanup() {
        try {
            productRepository.deleteAllInBatch();
            log.debug("Cleaned up existing products");
        } catch (Exception e) {
            log.warn("Failed to cleanup products: {}", e.getMessage());
        }
    }

    @Override
    public int order() {
        return 6;
    }

    @Override
    public String name() {
        return "ProductSeeder";
    }

    /**
     * Build product with null-safe relationship lookups.
     * Skips product if required relationships missing.
     */
    private Optional<Product> buildProduct(com.eshop.app.seed.model.ProductData cfg, SeederContext context) {
        // Validate required references exist
        Category category = context.getCategories().get(cfg.categoryName());
        if (category == null) {
            log.warn("Skipping product '{}': category '{}' not found",
                    cfg.name(), cfg.categoryName());
            return Optional.empty();
        }

        Store store = context.getStores().get(cfg.storeName());
        if (store == null) {
            log.warn("Skipping product '{}': store '{}' not found",
                    cfg.name(), cfg.storeName());
            return Optional.empty();
        }

        // Optional references
        Brand brand = context.getBrands().get(cfg.brandName());

        Set<Tag> tags = Optional.ofNullable(cfg.tags())
                .orElse(Collections.emptyList())
                .stream()
                .map(tagName -> context.getTags().get(tagName))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return Optional.of(Product.builder()
                .name(cfg.name())
                .description(cfg.description())
                .sku(cfg.sku())
                .price(BigDecimal.valueOf(cfg.price()))
                .discountPrice(BigDecimal.valueOf(cfg.discountPrice()))
                // .stockQuantity(Objects.requireNonNullElse(cfg.stockQuantity(), 0)) // Model
                // doesn't have stockQuantity currently, assuming default or need to add
                .stockQuantity(100) // Default stock as it was missed in model record creation, using safe default
                .category(category)
                .brand(brand)
                .store(store)
                .tags(tags)
                // .featured(cfg.isFeatured()) // Missed in model
                .featured(false)
                // .status(cfg.isActive() ? ProductStatus.ACTIVE : ProductStatus.INACTIVE) //
                // Missed in model
                .status(ProductStatus.ACTIVE)
                .build());
    }


}
