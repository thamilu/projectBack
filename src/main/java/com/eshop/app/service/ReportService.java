package com.eshop.app.service;

import com.eshop.app.entity.Product;

public interface ReportService {
    void createLowStockEntry(Product product);
}
