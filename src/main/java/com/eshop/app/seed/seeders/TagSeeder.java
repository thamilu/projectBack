package com.eshop.app.seed.seeders;

import com.eshop.app.entity.Tag;
import com.eshop.app.repository.TagRepository;
import com.eshop.app.seed.core.Seeder;
import com.eshop.app.seed.core.SeederContext;
import com.eshop.app.seed.exception.CatalogSeedingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tag seeder - Order 4.
 * Creates product tags.
 */
@Slf4j
@Component
@Order(4)
@RequiredArgsConstructor
public class TagSeeder implements Seeder<Tag, SeederContext> {
    
    private final TagRepository tagRepository;
    private final com.eshop.app.seed.provider.TagDataProvider tagDataProvider;
    
    @Override
    public List<Tag> seed(SeederContext context) {
        try {
            List<Tag> tags = tagDataProvider.getTags().stream()
                .map(this::buildTag)
                .toList();
            
            List<Tag> savedTags = tagRepository.saveAll(tags);
            
            // Populate context
            context.setTags(savedTags.stream()
                .collect(Collectors.toMap(Tag::getName, Function.identity(),
                    (existing, replacement) -> {
                        log.warn("Duplicate tag name: {}, keeping first", existing.getName());
                        return existing;
                    })));
            
            log.info("Seeded {} tags successfully", savedTags.size());
            return savedTags;
            
        } catch (DataAccessException e) {
            throw new CatalogSeedingException(
                "Failed to seed tags: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void cleanup() {
        try {
            tagRepository.deleteAllInBatch();
            log.debug("Cleaned up existing tags");
        } catch (Exception e) {
            log.warn("Failed to cleanup tags: {}", e.getMessage());
        }
    }
    
    @Override
    public int order() {
        return 4;
    }
    
    @Override
    public String name() {
        return "TagSeeder";
    }
    
    private Tag buildTag(com.eshop.app.seed.model.TagData cfg) {
        return Tag.builder()
            .name(cfg.name())
            .build();
    }
}
