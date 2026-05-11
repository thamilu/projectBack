package com.eshop.app.seed.provider;

import com.eshop.app.seed.model.StoreData;
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
 * JSON-based implementation of StoreDataProvider.
 */
@Slf4j
@Component
@Profile({"dev", "test", "local"})
@RequiredArgsConstructor
public class JsonFileStoreDataProvider implements StoreDataProvider {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Override
    public List<StoreData> getStores() {
        try {
            Resource resource = resourceLoader.getResource("classpath:seed/stores.json");
            if (!resource.exists()) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(resource.getInputStream(), new TypeReference<List<StoreData>>() {});
        } catch (IOException e) {
            log.error("Failed to load store seed data: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
