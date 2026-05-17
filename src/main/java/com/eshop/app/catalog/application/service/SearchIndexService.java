package com.eshop.app.catalog.application.service;

import com.eshop.app.catalog.domain.entity.Product;





/**
 * Service for managing the search index (e.g. PostgreSQL tsvector or ElasticSearch).
 */
public interface SearchIndexService {
    void indexProduct(Product product);
}
