package com.eshop.app.seed.provider;

import com.eshop.app.seed.model.ProductData;
import java.util.List;

public interface ProductDataProvider {
    List<ProductData> getProducts();
}
