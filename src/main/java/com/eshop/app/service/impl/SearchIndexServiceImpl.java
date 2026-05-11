package com.eshop.app.service.impl;

import com.eshop.app.entity.Product;
import com.eshop.app.service.SearchIndexService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SearchIndexServiceImpl implements SearchIndexService {
    @Override
    public void indexProduct(Product product) {
        log.info("Updating search index for product: {} (ID: {})", product.getName(), product.getId());
        // In a real implementation, this might push to ElasticSearch or update a tsvector in PostgreSQL.
        // For now, we rely on the database triggers or direct repository full-text search.
    }
}
