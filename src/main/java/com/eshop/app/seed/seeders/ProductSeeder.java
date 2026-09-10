package com.eshop.app.seed.seeders;

import com.eshop.app.catalog.domain.repository.ProductRepository;
import com.eshop.app.catalog.domain.entity.ProductStatus;
import com.eshop.app.catalog.domain.entity.Product;
import com.eshop.app.catalog.domain.entity.ProductImage;
import com.eshop.app.catalog.domain.entity.Category;
import com.eshop.app.catalog.domain.entity.Brand;
import com.eshop.app.store.domain.entity.Store;
import com.eshop.app.catalog.domain.entity.Tag;
import com.eshop.app.seed.core.BaseSeeder;
import com.eshop.app.seed.core.SeederContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
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
public class ProductSeeder extends BaseSeeder<Product, SeederContext> {

    private final ProductRepository productRepository;
    private final com.eshop.app.seed.provider.ProductDataProvider productDataProvider;

    @Override
    protected List<Product> doSeed(SeederContext context) {
        List<Product> products = productDataProvider.getProducts().stream()
                .map(cfg -> buildProduct(cfg, context))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        return productRepository.saveAll(products);
    }

    @Override
    protected void doCleanup() {
        productRepository.deleteAllInBatch();
    }

    @Override
    public int order() {
        return 6;
    }

    /**
     * Build product with null-safe relationship lookups.
     * Skips product if required relationships missing.
     */
    private Optional<Product> buildProduct(com.eshop.app.seed.model.ProductData cfg, SeederContext context) {
        // Resolve required dependencies using context helpers (fail-fast)
        Category category = context.getRequiredCategory(cfg.categoryName());
        Store store = context.getRequiredStore(cfg.storeName());

        // Optional references
        Brand brand = context.getBrands().get(cfg.brandName());

        Set<Tag> tags = Optional.ofNullable(cfg.tags())
                .orElse(Collections.emptyList())
                .stream()
                .map(tagName -> context.getTags().get(tagName))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Product product = Product.builder()
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
                // Previously hardcoded false regardless of the JSON — the homepage's
                // "Featured Products" section (filters on featured=true) therefore
                // always fell through to 100% demo/placeholder data, no matter how
                // large the seeded catalog was. Now driven by the seed file.
                .featured(Boolean.TRUE.equals(cfg.featured()))
                // .status(cfg.isActive() ? ProductStatus.ACTIVE : ProductStatus.INACTIVE) //
                // Missed in model
                .status(ProductStatus.ACTIVE)
                .build();

        // Images are a separate relation (ProductImage), not a scalar column —
        // addImage() must run after build() since it mutates the built instance
        // (adds to its `images` list and sets `primaryImage` on the first call).
        // cascade=ALL on Product.images means these persist together with the
        // product on the saveAll() below — no separate repository needed.
        List<String> imageUrls = Optional.ofNullable(cfg.imageUrls()).orElse(Collections.emptyList());
        for (int i = 0; i < imageUrls.size(); i++) {
            product.addImage(ProductImage.builder()
                    .url(imageUrls.get(i))
                    .altText(cfg.name())
                    .sortOrder(i)
                    .isPrimary(i == 0)
                    .build());
        }

        return Optional.of(product);
    }

}
