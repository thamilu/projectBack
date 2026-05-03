package com.eshop.app.service;

import com.eshop.app.entity.enums.CategoryType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class AttributeValidatorService {

    /**
     * Validates product attributes as a flexible key-value map.
     * Basic validation ensures map is not excessively large.
     */
    public void validateAttributes(CategoryType categoryType, Map<String, String> attributes) {
        if (categoryType == null || attributes == null || attributes.isEmpty()) {
            return; // nothing to validate
        }

        // Basic validation: check map size
        if (attributes.size() > 50) {
            throw new IllegalArgumentException("Too many attributes (max 50)");
        }

        // Basic validation: check key/value lengths
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                throw new IllegalArgumentException("Attribute key cannot be empty");
            }
            if (entry.getKey().length() > 100) {
                throw new IllegalArgumentException("Attribute key too long (max 100 chars): " + entry.getKey());
            }
            if (entry.getValue() != null && entry.getValue().length() > 1000) {
                throw new IllegalArgumentException("Attribute value too long (max 1000 chars) for key: " + entry.getKey());
            }
        }

        log.debug("Validated {} attributes for category type {}", attributes.size(), categoryType);
    }
}
