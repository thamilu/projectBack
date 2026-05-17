package com.eshop.app.analytics.application.service.impl;

import com.eshop.app.analytics.application.service.ReportService;
import com.eshop.app.catalog.domain.entity.Product;




import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DefaultReportService implements ReportService {
    @Override
    public void createLowStockEntry(Product product) {
        log.info("Creating low stock entry in reports for product ID: {}", product.getId());
        // This could persist to a specialized reports table or external analytics service.
    }
}

