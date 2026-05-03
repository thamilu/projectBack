package com.eshop.app.seed.provider;

import com.eshop.app.seed.model.StoreData;
import java.util.List;

public interface StoreDataProvider {
    List<StoreData> getStores();
}
