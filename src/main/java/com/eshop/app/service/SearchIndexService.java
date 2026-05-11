package com.eshop.app.service;

import com.eshop.app.entity.Product;

/**
 * Service for managing the search index (e.g. PostgreSQL tsvector or ElasticSearch).
 */
public interface SearchIndexService {
    void indexProduct(Product product);
}
