package com.eshop.app.seed.provider;

import com.eshop.app.seed.model.TagData;
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
 * JSON-based implementation of TagDataProvider.
 */
@Slf4j
@Component
@Profile({"dev", "test", "local"})
@RequiredArgsConstructor
public class JsonFileTagDataProvider implements TagDataProvider {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Override
    public List<TagData> getTags() {
        try {
            Resource resource = resourceLoader.getResource("classpath:seed/tags.json");
            if (!resource.exists()) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(resource.getInputStream(), new TypeReference<List<TagData>>() {});
        } catch (IOException e) {
            log.error("Failed to load tag seed data: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
