package com.eshop.app.seed.core;

import com.eshop.app.entity.*;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Context object passed between seeders to share saved entities.
 * Avoids repository queries and maintains referential integrity.
 */
@Data
@Builder
public class SeederContext {

    @Builder.Default
    private Map<String, User> users = new HashMap<>();

    @Builder.Default
    private Map<String, Category> categories = new HashMap<>();

    @Builder.Default
    private Map<String, Brand> brands = new HashMap<>();

    @Builder.Default
    private Map<String, Tag> tags = new HashMap<>();

    @Builder.Default
    private Map<String, Store> stores = new HashMap<>();
}
