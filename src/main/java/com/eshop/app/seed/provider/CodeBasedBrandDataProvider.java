package com.eshop.app.seed.provider;

import com.eshop.app.seed.model.BrandData;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile({"dev", "test", "local"})
public class CodeBasedBrandDataProvider implements BrandDataProvider {

    @Override
    public List<BrandData> getBrands() {
        return List.of(
            BrandData.of("Samsung", "Global electronics giant", null),
            BrandData.of("Nike", "Leading sports brand", null),
            BrandData.of("Apple", "Premium technology products", null),
            BrandData.of("Adidas", "Sports and lifestyle brand", null),
            BrandData.of("Puma", "Athletic and casual footwear", null),
            BrandData.of("Sony", "Electronics and entertainment", null),
            BrandData.of("LG", "Life's Good - Electronics", null),
            BrandData.of("H&M", "Fashion and clothing", null),
            BrandData.of("Zara", "Trendy fashion apparel", null),
            BrandData.of("Dell", "Computer technology", null)
        );
    }
}
