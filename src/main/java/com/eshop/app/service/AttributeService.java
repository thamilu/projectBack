package com.eshop.app.service;

import com.eshop.app.entity.AttributeDefinition;
import com.eshop.app.entity.CategoryAttributeMapping;

import java.util.List;
import java.util.Map;

public interface AttributeService {

    /**
     * Create or update an attribute definition.
     */
    AttributeDefinition saveAttributeDefinition(AttributeDefinition definition);

    /**
     * Get all attribute definitions.
     */
    List<AttributeDefinition> getAllAttributeDefinitions();

    /**
     * Map an attribute to a category.
     */
    CategoryAttributeMapping mapAttributeToCategory(Long categoryId, Long attributeId, boolean required, int displayOrder);

    /**
     * Get all attributes for a category.
     */
    List<CategoryAttributeMapping> getAttributesByCategory(Long categoryId);

    /**
     * Validate product attributes against the category rules.
     * @throws IllegalArgumentException if validation fails
     */
    void validateProductAttributes(Long categoryId, Map<String, String> attributes);
}
