package com.eshop.app.seed.seeders;

import com.eshop.app.catalog.domain.entity.Tag;
import com.eshop.app.catalog.domain.repository.TagRepository;

import com.eshop.app.seed.core.BaseSeeder;
import com.eshop.app.seed.core.SeederContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tag seeder - Order 4.
 * Creates product tags.
 */
@Slf4j
@Component
@Order(4)
@RequiredArgsConstructor
public class TagSeeder extends BaseSeeder<Tag, SeederContext> {

    private final TagRepository tagRepository;
    private final com.eshop.app.seed.provider.TagDataProvider tagDataProvider;

    @Override
    protected List<Tag> doSeed(SeederContext context) {
        List<Tag> tags = tagDataProvider.getTags().stream()
                .map(this::buildTag)
                .toList();

        List<Tag> savedTags = tagRepository.saveAll(tags);

        // Populate context
        savedTags.forEach(t -> context.getTags().put(t.getName(), t));

        return savedTags;
    }

    @Override
    protected void doCleanup() {
        tagRepository.deleteAllInBatch();
    }

    @Override
    public int order() {
        return 4;
    }

    private Tag buildTag(com.eshop.app.seed.model.TagData cfg) {
        return Tag.builder()
                .name(cfg.name())
                .build();
    }
}
