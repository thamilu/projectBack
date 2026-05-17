package com.eshop.app.analytics.application.service;

import com.eshop.app.catalog.domain.entity.Product;





public interface ReportService {
    void createLowStockEntry(Product product);
}
