package com.eshop.app.seed.provider;

import com.eshop.app.seed.model.StoreData;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile({"dev", "test", "local"})
public class CodeBasedStoreDataProvider implements StoreDataProvider {

    @Override
    public List<StoreData> getStores() {
        return List.of(
            StoreData.full(
                "Tech Retail Store", 
                "retail1", 
                "Best electronics for consumers - retail prices only",
                "RETAILER"
            ),
            StoreData.full(
                "Mega Wholesale Center", 
                "wholesale1", 
                "Bulk products for businesses - wholesale prices only",
                "WHOLESALER"
            ),
            StoreData.full(
                "FlexiMart", 
                "shop1", 
                "One-stop shop - retail for small orders, wholesale for bulk",
                "BUSINESS"
            ),
            StoreData.full(
                "Green Valley Farm", 
                "farmer1", 
                "Fresh organic vegetables and fruits directly from farm",
                "BUSINESS"
            )
        );
    }
}
