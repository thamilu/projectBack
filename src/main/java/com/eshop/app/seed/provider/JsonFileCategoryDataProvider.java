package com.eshop.app.seed.provider;

import com.eshop.app.seed.model.CategoryNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JSON-based implementation of CategoryDataProvider.
 * Loads category hierarchy from src/main/resources/seed/categories.json.
 */
@Slf4j
@Component
@Profile({"dev", "test", "local"})
@RequiredArgsConstructor
public class JsonFileCategoryDataProvider implements CategoryDataProvider {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Override
    public List<CategoryNode> getCategoryHierarchy() {
        try {
            Resource resource = resourceLoader.getResource("classpath:seed/categories.json");
            if (!resource.exists()) {
                log.warn("Seed data file not found: classpath:seed/categories.json");
                return Collections.emptyList();
            }
            
            return objectMapper.readValue(resource.getInputStream(), new TypeReference<List<CategoryNode>>() {});
        } catch (IOException e) {
            log.error("Failed to load category seed data from JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public String getProviderName() {
        return "json-file";
    }
}
