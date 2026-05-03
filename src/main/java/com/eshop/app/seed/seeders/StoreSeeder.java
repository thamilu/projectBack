package com.eshop.app.seed.seeders;


import com.eshop.app.entity.Store;
import com.eshop.app.entity.User;
import com.eshop.app.repository.StoreRepository;
import com.eshop.app.seed.core.Seeder;
import com.eshop.app.seed.core.SeederContext;
import com.eshop.app.seed.exception.StoreSeedingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Store seeder - Order 5.
 * Creates stores associated with seller users.
 * Depends on UserSeeder.
 */
@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class StoreSeeder implements Seeder<Store, SeederContext> {

    private final StoreRepository storeRepository;
    private final com.eshop.app.seed.provider.StoreDataProvider storeDataProvider;

    private final com.eshop.app.repository.SellerProfileRepository sellerProfileRepository;

    @Override
    public List<Store> seed(SeederContext context) {
        try {
            Map<String, User> users = context.getUsers();
            
            // Pre-fetch all existing seller profiles to avoid N+1 queries during the loop
            Map<Long, com.eshop.app.entity.SellerProfile> existingProfiles = sellerProfileRepository.findAll().stream()
                    .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));

            List<Store> storesList = storeDataProvider.getStores().stream()
                    .map(cfg -> buildStoreWithCache(cfg, users, existingProfiles))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();

            List<Store> savedStores = storeRepository.saveAll(storesList);

            // Populate context
            context.setStores(savedStores.stream()
                    .collect(Collectors.toMap(Store::getStoreName, Function.identity(),
                            (existing, replacement) -> {
                                log.warn("Duplicate store name: {}, keeping first", existing.getStoreName());
                                return existing;
                            })));

            log.info("Seeded {} stores successfully", savedStores.size());
            return savedStores;

        } catch (DataAccessException e) {
            throw new StoreSeedingException(
                    "Failed to seed stores: " + e.getMessage(), e);
        }
    }

    @Override
    public void cleanup() {
        try {
            storeRepository.deleteAllInBatch();
            log.debug("Cleaned up existing stores");
        } catch (Exception e) {
            log.warn("Failed to cleanup stores: {}", e.getMessage());
        }
    }

    @Override
    public int order() {
        return 5;
    }

    @Override
    public String name() {
        return "StoreSeeder";
    }

    /**
     * Build store with null-safe seller lookup.
     * Skips store if seller not found.
     */
    private Optional<Store> buildStoreWithCache(com.eshop.app.seed.model.StoreData cfg, 
                                                Map<String, User> users,
                                                Map<Long, com.eshop.app.entity.SellerProfile> existingProfiles) {
        User seller = users.get(cfg.sellerUsername());

        if (seller == null) {
            log.warn("Skipping store '{}': seller '{}' not found",
                    cfg.storeName(), cfg.sellerUsername());
            return Optional.empty();
        }

        com.eshop.app.entity.SellerProfile profile = existingProfiles.get(seller.getId());
        if (profile == null) {
            com.eshop.app.enums.SellerIdentityType identityType;
            try {
                identityType = com.eshop.app.enums.SellerIdentityType.valueOf(cfg.sellerType().toUpperCase());
            } catch (Exception e) {
                identityType = com.eshop.app.enums.SellerIdentityType.BUSINESS;
            }

            profile = com.eshop.app.entity.SellerProfile.builder()
                    .user(seller)
                    .businessName(cfg.storeName())
                    .status(com.eshop.app.enums.SellerStatus.ACTIVE)
                    .identityType(identityType)
                    .build();
            // We still need to save it to get an ID for the Store relationship if it's new
            profile = sellerProfileRepository.save(profile);
            existingProfiles.put(seller.getId(), profile);
        }

        return Optional.of(Store.builder()
                .storeName(cfg.storeName())
                .description(cfg.description())
                .addressLine1(cfg.address())
                .phone(cfg.phone())
                .logoUrl(cfg.logoUrl())
                .sellerProfile(profile)
                .active(true)
                .build());
    }
}
