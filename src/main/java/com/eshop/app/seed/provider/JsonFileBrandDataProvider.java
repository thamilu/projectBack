package com.eshop.app.seed.provider;

import com.eshop.app.seed.model.BrandData;
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
 * JSON-based implementation of BrandDataProvider.
 * Loads brand data from src/main/resources/seed/brands.json.
 */
@Slf4j
@Component
@Profile({"dev", "test", "local"})
@RequiredArgsConstructor
public class JsonFileBrandDataProvider implements BrandDataProvider {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Override
    public List<BrandData> getBrands() {
        try {
            Resource resource = resourceLoader.getResource("classpath:seed/brands.json");
            if (!resource.exists()) {
                log.warn("Seed data file not found: classpath:seed/brands.json");
                return Collections.emptyList();
            }
            
            return objectMapper.readValue(resource.getInputStream(), new TypeReference<List<BrandData>>() {});
        } catch (IOException e) {
            log.error("Failed to load brand seed data from JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
