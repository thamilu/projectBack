package com.eshop.app.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * Externalized seed data configuration for development/testing.
 * Populate `app.seed.*` in application-dev.properties or environment-specific config.
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.seed")
public class SeedProperties {

    /** Enable/disable seeding (default: true for dev profile) */
    private boolean enabled = true;

    private boolean usersEnabled = true;
    private boolean categoriesEnabled = true;
    private boolean brandsEnabled = true;
    private boolean tagsEnabled = true;
    private boolean shopsEnabled = true;
    private boolean productsEnabled = true;

    /** Users to seed */
    @Valid
    private List<UserSeed> users = new ArrayList<>();

    @Getter
    @Setter
    public static class UserSeed {
        @NotBlank
        private String username;
        private String email;
        private String password; // optional: if absent, DataSeeder will generate one or use env override
        private String firstName;
        private String lastName;
        private String phone;
        private String address;
        private String role; // ADMIN, SELLER, CUSTOMER, DELIVERY_AGENT
        private String sellerType; // INDIVIDUAL, BUSINESS, FARMER, WHOLESALER, RETAILER
    }
}
