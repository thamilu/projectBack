package com.eshop.app.seller.application.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import com.eshop.app.seller.api.response.BusinessTypeResponse;
import com.eshop.app.seller.application.mapper.SellerMapper;
import com.eshop.app.store.domain.repository.StoreRepository;
import com.eshop.app.user.application.service.KeycloakService;
import com.eshop.app.user.domain.repository.SellerProfileRepository;
import com.eshop.app.user.domain.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SellerAdminServiceImplTest {

    private SellerProfileRepository sellerProfileRepository;
    private UserRepository userRepository;
    private StoreRepository storeRepository;
    private KeycloakService keycloakService;
    private AppProperties appProperties;
    private SellerMapper sellerMapper;
    private com.eshop.app.store.application.mapper.StoreMapper storeMapper;

    private SellerAdminServiceImpl sellerAdminService;

    @BeforeEach
    void setUp() {
        sellerProfileRepository = mock(SellerProfileRepository.class);
        userRepository = mock(UserRepository.class);
        storeRepository = mock(StoreRepository.class);
        keycloakService = mock(KeycloakService.class);
        appProperties = mock(AppProperties.class);
        sellerMapper = mock(SellerMapper.class);
        storeMapper = mock(com.eshop.app.store.application.mapper.StoreMapper.class);

        sellerAdminService =
                new SellerAdminServiceImpl(
                        sellerProfileRepository,
                        userRepository,
                        storeRepository,
                        keycloakService,
                        appProperties,
                        sellerMapper,
                        storeMapper);
    }

    @Test
    void getBusinessTypes_returnsTypedList() {
        List<BusinessTypeResponse> businessTypes = sellerAdminService.getBusinessTypes();

        assertNotNull(businessTypes);
        assertFalse(businessTypes.isEmpty());
        assertEquals("FARMER", businessTypes.get(0).getCode());
        assertEquals("Farmer / Producer", businessTypes.get(0).getDisplayName());
    }

    @Test
    void approveSeller_whenSellerIdNull_throwsIllegalArgumentException() {
        IllegalArgumentException ex =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> sellerAdminService.approveSeller(null, "admin"));
        assertEquals("sellerId must not be null", ex.getMessage());
    }

    @Test
    void approveSeller_whenProcessedByNullOrBlank_throwsIllegalArgumentException() {
        IllegalArgumentException ex1 =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> sellerAdminService.approveSeller(1L, null));
        assertEquals("processedBy must not be null or blank", ex1.getMessage());

        IllegalArgumentException ex2 =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> sellerAdminService.approveSeller(1L, "   "));
        assertEquals("processedBy must not be null or blank", ex2.getMessage());
    }

    @Test
    void rejectSeller_whenSellerIdNull_throwsIllegalArgumentException() {
        IllegalArgumentException ex =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> sellerAdminService.rejectSeller(null, "Reason", "admin"));
        assertEquals("sellerId must not be null", ex.getMessage());
    }

    @Test
    void rejectSeller_whenRejectionReasonNullOrBlank_throwsIllegalArgumentException() {
        IllegalArgumentException ex1 =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> sellerAdminService.rejectSeller(1L, null, "admin"));
        assertEquals("rejectionReason must not be null or blank", ex1.getMessage());

        IllegalArgumentException ex2 =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> sellerAdminService.rejectSeller(1L, "  ", "admin"));
        assertEquals("rejectionReason must not be null or blank", ex2.getMessage());
    }

    @Test
    void rejectSeller_whenProcessedByNullOrBlank_throwsIllegalArgumentException() {
        IllegalArgumentException ex1 =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> sellerAdminService.rejectSeller(1L, "Reason", null));
        assertEquals("processedBy must not be null or blank", ex1.getMessage());

        IllegalArgumentException ex2 =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> sellerAdminService.rejectSeller(1L, "Reason", "  "));
        assertEquals("processedBy must not be null or blank", ex2.getMessage());
    }

    @Test
    void syncSellerRole_whenSellerIdNull_throwsIllegalArgumentException() {
        IllegalArgumentException ex =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> sellerAdminService.syncSellerRole(null));
        assertEquals("sellerId must not be null", ex.getMessage());
    }
}
