package com.eshop.app.seller.application.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eshop.app.seller.api.request.SellerRegisterRequest;
import com.eshop.app.seller.application.mapper.SellerMapper;
import com.eshop.app.user.domain.repository.SellerProfileRepository;
import com.eshop.app.user.domain.repository.UserRepository;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class RegisterSellerUseCaseImplTest {

    @Test
    void registerSeller_whenUserIdNull_throwsIllegalArgumentException() {
        SellerProfileRepository sellerProfileRepository = mock(SellerProfileRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        SellerMapper sellerMapper = mock(SellerMapper.class);
        com.eshop.app.user.application.service.ProfileSyncService profileSyncService =
                mock(com.eshop.app.user.application.service.ProfileSyncService.class);

        RegisterSellerUseCaseImpl service =
                new RegisterSellerUseCaseImpl(
                        sellerProfileRepository,
                        userRepository,
                        sellerMapper,
                        profileSyncService,
                        Collections.emptyList(),
                        Collections.emptyList());

        SellerRegisterRequest request = new SellerRegisterRequest();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.registerSeller(null, request));

        assertEquals("userId must not be null", exception.getMessage());
    }

    @Test
    void registerSeller_whenRequestNull_throwsIllegalArgumentException() {
        SellerProfileRepository sellerProfileRepository = mock(SellerProfileRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        SellerMapper sellerMapper = mock(SellerMapper.class);
        com.eshop.app.user.application.service.ProfileSyncService profileSyncService =
                mock(com.eshop.app.user.application.service.ProfileSyncService.class);

        RegisterSellerUseCaseImpl service =
                new RegisterSellerUseCaseImpl(
                        sellerProfileRepository,
                        userRepository,
                        sellerMapper,
                        profileSyncService,
                        Collections.emptyList(),
                        Collections.emptyList());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class, () -> service.registerSeller(1L, null));

        assertEquals("request must not be null", exception.getMessage());
    }
}
