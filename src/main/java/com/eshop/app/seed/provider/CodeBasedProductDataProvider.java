package com.eshop.app.seed.provider;

import com.eshop.app.seed.model.ProductData;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile({"dev", "test", "local"})
public class CodeBasedProductDataProvider implements ProductDataProvider {

    @Override
    public List<ProductData> getProducts() {
        return List.of(
            ProductData.of(
                "Samsung Galaxy S24", 
                "SAMSUNG-S24-001", 
                999.99, 
                899.99, 
                "Electronics", 
                "Samsung", 
                "Tech Retail Store", 
                "new", "popular"
            ),
            ProductData.of(
                "Nike Air Max", 
                "NIKE-AIRMAX-001", 
                150.00, 
                120.00, 
                        "Men's Sports Shoes",
                "Nike", 
                "Tech Retail Store", 
                "sale"
            )
        );
    }
}
