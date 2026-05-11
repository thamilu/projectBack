package com.eshop.app.seed.seeders;


import com.eshop.app.entity.Store;
import com.eshop.app.entity.User;
import com.eshop.app.repository.StoreRepository;
import com.eshop.app.seed.core.BaseSeeder;
import com.eshop.app.seed.core.SeederContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
public class StoreSeeder extends BaseSeeder<Store, SeederContext> {

    private final StoreRepository storeRepository;
    private final com.eshop.app.seed.provider.StoreDataProvider storeDataProvider;
    private final com.eshop.app.repository.SellerProfileRepository sellerProfileRepository;
    private final com.eshop.app.config.properties.SeedProperties seedProperties;

    @Override
    protected List<Store> doSeed(SeederContext context) {
        // Pre-fetch all existing seller profiles to avoid N+1 queries during the loop
        Map<Long, com.eshop.app.entity.SellerProfile> existingProfiles = sellerProfileRepository.findAll().stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));

        List<Store> storesList = storeDataProvider.getStores().stream()
                .map(cfg -> buildStoreWithCache(cfg, context, existingProfiles))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        List<Store> savedStores = storeRepository.saveAll(storesList);

        // Populate context
        savedStores.forEach(s -> context.getStores().put(s.getStoreName(), s));

        return savedStores;
    }

    @Override
    protected void doCleanup() {
        storeRepository.deleteAllInBatch();
    }

    @Override
    public int order() {
        return 5;
    }

    /**
     * Build store with null-safe seller lookup.
     * Skips store if seller not found.
     */
    private Optional<Store> buildStoreWithCache(com.eshop.app.seed.model.StoreData cfg, 
                                                SeederContext context,
                                                Map<Long, com.eshop.app.entity.SellerProfile> existingProfiles) {
        User seller = context.getRequiredUser(cfg.sellerEmail());

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
                    .shopName(cfg.storeName())
                    .shopHandle(generateShopHandle(cfg, seller))
                    .addressLine1(Optional.ofNullable(cfg.address()).orElse(seedProperties.getDefaultAddress()))
                    .city(Optional.ofNullable(cfg.city()).orElse(seedProperties.getDefaultCity()))
                    .state(Optional.ofNullable(cfg.state()).orElse(seedProperties.getDefaultState()))
                    .pincode(Optional.ofNullable(cfg.pincode()).orElse(seedProperties.getDefaultPincode()))
                    .country(Optional.ofNullable(cfg.country()).orElse(seedProperties.getDefaultCountry()))
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
                .addressLine1(Optional.ofNullable(cfg.address()).orElse(seedProperties.getDefaultAddress()))
                .phone(cfg.phone())
                .logoUrl(cfg.logoUrl())
                .sellerProfile(profile)
                .active(true)
                .build());
    }

    private String generateShopHandle(com.eshop.app.seed.model.StoreData cfg, User seller) {
        return cfg.storeName().toLowerCase()
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "")
                + "-" + seller.getId();
    }
}
