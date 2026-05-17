package com.eshop.app.seed.seeders;

import com.eshop.app.catalog.domain.entity.Brand;
import com.eshop.app.catalog.domain.repository.BrandRepository;

import com.eshop.app.seed.core.BaseSeeder;
import com.eshop.app.seed.core.SeederContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Brand seeder - Order 3.
 * Creates product brands.
 */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class BrandSeeder extends BaseSeeder<Brand, SeederContext> {

    private final BrandRepository brandRepository;
    private final com.eshop.app.seed.provider.BrandDataProvider brandDataProvider;

    @Override
    protected List<Brand> doSeed(SeederContext context) {
        List<Brand> brands = brandDataProvider.getBrands().stream()
                .map(this::buildBrand)
                .toList();

        List<Brand> savedBrands = brandRepository.saveAll(brands);

        // Populate context
        savedBrands.forEach(b -> context.getBrands().put(b.getName(), b));

        return savedBrands;
    }

    @Override
    protected void doCleanup() {
        brandRepository.deleteAllInBatch();
    }

    @Override
    public int order() {
        return 3;
    }

    private Brand buildBrand(com.eshop.app.seed.model.BrandData cfg) {
        return Brand.builder()
                .name(cfg.name())
                .description(cfg.description())
                .logoUrl(cfg.logoUrl())
                .active(true)
                .build();
    }
}
