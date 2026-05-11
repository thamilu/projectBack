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

    // --- Helper Methods for DRY Seeding ---

    public Category getRequiredCategory(String name) {
        Category category = categories.get(name);
        if (category == null) {
            throw new com.eshop.app.seed.exception.SeedingException(
                "Missing required dependency: Category '" + name + "'. Ensure CategorySeeder runs first.", 
                com.eshop.app.seed.exception.SeedingException.SeedPhase.CATEGORY_SEEDING);
        }
        return category;
    }

    public Store getRequiredStore(String name) {
        Store store = stores.get(name);
        if (store == null) {
            throw new com.eshop.app.seed.exception.SeedingException(
                "Missing required dependency: Store '" + name + "'. Ensure StoreSeeder runs before products.", 
                com.eshop.app.seed.exception.SeedingException.SeedPhase.STORE_SEEDING);
        }
        return store;
    }

    public User getRequiredUser(String email) {
        User user = users.get(email);
        if (user == null) {
            throw new com.eshop.app.seed.exception.SeedingException(
                "Missing required dependency: User '" + email + "'. Ensure UserSeeder runs first.", 
                com.eshop.app.seed.exception.SeedingException.SeedPhase.USER_SEEDING);
        }
        return user;
    }
}
