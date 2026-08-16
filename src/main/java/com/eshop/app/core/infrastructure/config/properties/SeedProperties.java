package com.eshop.app.core.infrastructure.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Externalized seed data configuration for development/testing.
 * Populate `app.seed.*` in application-dev.properties or environment-specific
 * config.
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.seed")
public class SeedProperties {

    /** Enable/disable seeding (default: true for dev profile) */
    private boolean enabled = true;

    /**
     * When false (default), seeding is skipped if the database already has users
     * from a prior run. Set to true to wipe and reseed on every startup.
     */
    private boolean forceReseed = false;

    private boolean usersEnabled = true;
    private boolean categoriesEnabled = true;
    private boolean brandsEnabled = true;
    private boolean tagsEnabled = true;
    private boolean shopsEnabled = true;
    private boolean productsEnabled = true;
    private boolean locationsEnabled = true;

    private String pincodeCsvPath;

    private String defaultCity = "Seed City";
    private String defaultState = "Seed State";
    private String defaultCountry = "India";
    private String defaultPincode = "000000";
    private String defaultAddress = "N/A";

    // Users are now managed via UserDataProvider (json)

    @Getter
    @Setter
    public static class UserSeed {
        @NotBlank
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
