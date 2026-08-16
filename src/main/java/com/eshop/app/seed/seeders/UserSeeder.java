package com.eshop.app.seed.seeders;

import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.domain.repository.UserRepository;
import com.eshop.app.user.domain.repository.UserProfileRepository;
import com.eshop.app.user.domain.repository.SellerProfileRepository;
import com.eshop.app.admin.application.service.KeycloakAdminService;
import com.eshop.app.user.shared.domain.enums.UserRole;

import com.eshop.app.core.infrastructure.config.properties.SeedProperties;
import com.eshop.app.user.api.request.RegisterRequest;
import com.eshop.app.seed.core.BaseSeeder;
import com.eshop.app.seed.core.SeederContext;
import com.eshop.app.seed.security.SecurePasswordGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
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
public class UserSeeder extends BaseSeeder<User, SeederContext> {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final com.eshop.app.user.domain.repository.UserAddressRepository userAddressRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final com.eshop.app.seller.domain.repository.SellerKYCRepository sellerKYCRepository;
    private final com.eshop.app.seller.domain.repository.SellerFarmerDetailsRepository sellerFarmerDetailsRepository;
    private final com.eshop.app.seller.domain.repository.SellerBusinessDetailsRepository sellerBusinessDetailsRepository;
    private final com.eshop.app.seller.domain.repository.SellerWholesaleConfigRepository sellerWholesaleConfigRepository;
    private final com.eshop.app.seller.domain.repository.SellerBankAccountRepository sellerBankAccountRepository;
    private final com.eshop.app.seller.domain.repository.SellerDocumentRepository sellerDocumentRepository;
    private final SecurePasswordGenerator passwordGenerator;
    private final SeedProperties seedProperties;
    private final KeycloakAdminService keycloakAdminService;
    private final com.eshop.app.seed.provider.UserDataProvider userDataProvider;

    @Override
    protected List<User> doSeed(SeederContext context) {
        // If disabled, just load existing users to context so other seeders can
        // function
        if (!seedProperties.isUsersEnabled()) {
            log.info("User seeding disabled. Loading existing users into context...");
            List<User> existingUsers = userRepository.findAll();
            // Populate context for downstream seeders
            existingUsers.forEach(u -> context.getUsers().put(u.getEmail(), u));
            return existingUsers;
        }

        List<User> savedUsers = new java.util.ArrayList<>();

        for (SeedProperties.UserSeed cfg : userDataProvider.getUsers()) {
            try {
                User user = processAndBuildUser(cfg);
                savedUsers.add(user);
                context.getUsers().put(cfg.getEmail(), user);
            } catch (Exception e) {
                log.error("Failed to process seed user '{}': {}. Skipping...", cfg.getEmail(), e.getMessage());
            }
        }

        if (!savedUsers.isEmpty()) {
            userRepository.saveAll(savedUsers);
        }
        return savedUsers;
    }

    @Override
    protected void doCleanup() {
        if (!seedProperties.isUsersEnabled()) {
            return;
        }

        // Note: We only clean up local DB. Cleaning up Keycloak is risky/complex for
        // dev

        // 1. Clean up Seller Profile Children (children before parent to avoid FK
        // errors)
        sellerKYCRepository.deleteAllInBatch();
        sellerFarmerDetailsRepository.deleteAllInBatch();
        sellerBusinessDetailsRepository.deleteAllInBatch();
        sellerWholesaleConfigRepository.deleteAllInBatch();
        sellerBankAccountRepository.deleteAllInBatch();
        sellerDocumentRepository.deleteAllInBatch();

        // 2. Clean up Seller Profiles
        sellerProfileRepository.deleteAllInBatch();

        // 3. Clean up User Profile Children (addresses before profiles)
        userAddressRepository.deleteAllInBatch();

        // 4. Clean up User Profiles (before users, FK dependency)
        userProfileRepository.deleteAllInBatch();

        // 5. Clean up Users (last, no more FKs pointing to it)
        userRepository.deleteAllInBatch();
    }

    @Override
    public int order() {
        return 1;
    }

    /**
     * Process user: Create in Keycloak if needed, then build local entity.
     */
    private User processAndBuildUser(SeedProperties.UserSeed cfg) {
        // 1. Resolve Password
        String rawPassword = cfg.getPassword();
        if (rawPassword == null || rawPassword.isBlank()) {
            rawPassword = passwordGenerator.generate(cfg.getEmail());
        }

        // 2. Create in Keycloak
        Map<String, String> keycloakResult = createKeycloakUser(cfg, rawPassword);
        String keycloakId = keycloakResult != null ? keycloakResult.get("id") : "unknown-" + cfg.getEmail();

        User user = User.builder()
                .keycloakId(keycloakId)
                .email(cfg.getEmail())
                .role(parseRole(cfg.getRole()))
                .build();

        com.eshop.app.user.domain.entity.UserProfile profile = com.eshop.app.user.domain.entity.UserProfile.builder()
                .firstName(cfg.getFirstName())
                .lastName(cfg.getLastName())
                .phone(cfg.getPhone())
                .user(user)
                .build();

        if (cfg.getAddress() != null && !cfg.getAddress().isBlank()) {
            com.eshop.app.user.domain.entity.UserAddress address = com.eshop.app.user.domain.entity.UserAddress
                    .builder()
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
                    .email(cfg.getEmail())
                    .password(password)
                    .firstName(cfg.getFirstName())
                    .lastName(cfg.getLastName())
                    .enabled(true)
                    .build();

            // We use block() here because Seeding is a startup sync process
            return keycloakAdminService.createUser(request)
                    .doOnError(
                            e -> log.debug("User already exists or error creating in Keycloak: {}", cfg.getEmail()))
                    .onErrorResume(e -> {
                        return keycloakAdminService.getUserByEmail(cfg.getEmail())
                                .map(userMap -> {
                                    Map<String, String> res = new java.util.HashMap<>();
                                    res.put("id", (String) userMap.get("id"));
                                    res.put("email", cfg.getEmail());
                                    return res;
                                });
                    })
                    .block();

        } catch (Exception e) {
            // Log but don't fail the whole seeding - user might already exist
            log.warn("Failed to create/resolve Keycloak user '{}': {}", cfg.getEmail(), e.getMessage());
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
