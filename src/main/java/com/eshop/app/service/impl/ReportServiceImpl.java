package com.eshop.app.service.impl;

import com.eshop.app.entity.Product;
import com.eshop.app.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Override
    public void createLowStockEntry(Product product) {
        log.info("Creating low stock entry in reports for product ID: {}", product.getId());
        // This could persist to a specialized reports table or external analytics service.
    }
}
