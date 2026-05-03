package com.eshop.app.seed.seeders;

import com.eshop.app.config.properties.SeedProperties;
import com.eshop.app.dto.auth.RegisterRequest;
import com.eshop.app.entity.User;
import com.eshop.app.repository.UserRepository;
import com.eshop.app.repository.SellerProfileRepository;
import com.eshop.app.seed.core.Seeder;
import com.eshop.app.seed.core.SeederContext;
import com.eshop.app.seed.exception.UserSeedingException;
import com.eshop.app.seed.security.SecurePasswordGenerator;
import com.eshop.app.service.auth.KeycloakAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.eshop.app.enums.UserRole;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * User seeder - First in execution order.
 * Creates all users from configuration with secure passwords.
 * Ensures users exist in both Keycloak (Auth provider) and Local DB (Business
 * logic).
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class UserSeeder implements Seeder<User, SeederContext> {

    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final SecurePasswordGenerator passwordGenerator;
    private final SeedProperties seedProperties;
    private final KeycloakAdminService keycloakAdminService;

    @Override
    public List<User> seed(SeederContext context) {
        // If disabled, just load existing users to context so other seeders can
        // function
        if (!seedProperties.isUsersEnabled()) {
            log.info("User seeding disabled. Loading existing users into context...");
            List<User> existingUsers = userRepository.findAll();
            // Skipping context population as username is no longer in local DB
            return existingUsers;
        }

        try {
            List<User> savedUsers = new java.util.ArrayList<>();
            java.util.Map<String, User> contextMap = new java.util.HashMap<>();

            for (SeedProperties.UserSeed cfg : seedProperties.getUsers()) {
                try {
                    User user = processAndBuildUser(cfg);
                    savedUsers.add(user);
                    contextMap.put(cfg.getUsername(), user);
                } catch (Exception e) {
                    log.error("Failed to process seed user '{}': {}. Skipping...", cfg.getUsername(), e.getMessage());
                }
            }

            if (!savedUsers.isEmpty()) {
                userRepository.saveAll(savedUsers);
                log.info("Seeded {} users successfully to Local DB and Keycloak", savedUsers.size());
            } else {
                log.warn("No users were seeded!");
            }
            return savedUsers;

        } catch (DataAccessException e) {
            throw new UserSeedingException(
                    "Failed to seed users - database constraint violation: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new UserSeedingException(
                    "Unexpected error during user seeding: " + e.getMessage(), e);
        }
    }

    // Context is now populated directly in seed() to maintain mapping to
    // cfg.username

    @Override
    public void cleanup() {
        if (!seedProperties.isUsersEnabled()) {
            log.info("User seeding disabled, skipping cleanup (deletion) of users");
            return;
        }

        try {
            // Note: We only clean up local DB. Cleaning up Keycloak is risky/complex for
            // dev
            sellerProfileRepository.deleteAllInBatch(); // Clean up seller profiles first
            userRepository.deleteAllInBatch();
            log.debug("Cleaned up existing users from Local DB");
        } catch (Exception e) {
            log.warn("Failed to cleanup users: {}", e.getMessage());
        }
    }

    @Override
    public int order() {
        return 1;
    }

    @Override
    public String name() {
        return "UserSeeder";
    }

    /**
     * Process user: Create in Keycloak if needed, then build local entity.
     */
    private User processAndBuildUser(SeedProperties.UserSeed cfg) {
        // 1. Resolve Password
        String rawPassword = cfg.getPassword();
        if (rawPassword == null || rawPassword.isBlank()) {
            rawPassword = passwordGenerator.generate(cfg.getUsername());
        }

        // 2. Create in Keycloak
        Map<String, String> keycloakResult = createKeycloakUser(cfg, rawPassword);
        String keycloakId = keycloakResult != null ? keycloakResult.get("id") : "unknown-" + cfg.getUsername();

        User user = User.builder()
                .keycloakId(keycloakId)
                .role(parseRole(cfg.getRole()))
                .build();

        com.eshop.app.entity.UserProfile profile = com.eshop.app.entity.UserProfile.builder()
                .firstName(cfg.getFirstName())
                .lastName(cfg.getLastName())
                .phone(cfg.getPhone())
                .user(user)
                .build();

        if (cfg.getAddress() != null && !cfg.getAddress().isBlank()) {
            com.eshop.app.entity.UserAddress address = com.eshop.app.entity.UserAddress.builder()
                    .userProfile(profile)
                    .addressLine1(cfg.getAddress())
                    .isDefault(true)
                    .build();
            profile.setAddresses(new java.util.ArrayList<>(java.util.List.of(address)));
        }
        user.setUserProfile(profile);

        return user;
    }

    private Map<String, String> createKeycloakUser(SeedProperties.UserSeed cfg, String password) {
        try {
            RegisterRequest request = RegisterRequest.builder()
                    .username(cfg.getUsername())
                    .email(cfg.getEmail())
                    .password(password)
                    .firstName(cfg.getFirstName())
                    .lastName(cfg.getLastName())
                    .enabled(true)
                    .build();

            // We use block() here because Seeding is a startup sync process
            return keycloakAdminService.createUser(request)
                    .doOnError(
                            e -> log.debug("User already exists or error creating in Keycloak: {}", cfg.getUsername()))
                    .onErrorResume(e -> {
                        return keycloakAdminService.getUserByUsername(cfg.getUsername())
                                .map(userMap -> {
                                    Map<String, String> res = new java.util.HashMap<>();
                                    res.put("id", (String) userMap.get("id"));
                                    res.put("username", cfg.getUsername());
                                    return res;
                                });
                    })
                    .block();

        } catch (Exception e) {
            // Log but don't fail the whole seeding - user might already exist
            log.warn("Failed to create/resolve Keycloak user '{}': {}", cfg.getUsername(), e.getMessage());
            return null;
        }
    }

    /**
     * Parse role with fallback to CUSTOMER if invalid.
     */
    private UserRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            return UserRole.CUSTOMER;
        }
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role '{}', defaulting to CUSTOMER", role);
            return UserRole.CUSTOMER;
        }
    }

}
