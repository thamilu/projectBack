package com.eshop.app.catalog.application.service.impl;

import com.eshop.app.catalog.application.service.SearchIndexService;
import com.eshop.app.catalog.domain.entity.Product;




import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DefaultSearchIndexService implements SearchIndexService {
    @Override
    public void indexProduct(Product product) {
        log.info("Updating search index for product: {} (ID: {})", product.getName(), product.getId());
        // In a real implementation, this might push to ElasticSearch or update a tsvector in PostgreSQL.
        // For now, we rely on the database triggers or direct repository full-text search.
    }
}

