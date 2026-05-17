package com.eshop.app.seller.application.service;

import com.eshop.app.seller.api.request.SellerRegisterRequest;
import com.eshop.app.user.api.response.SellerProfileResponse;
import com.eshop.app.user.domain.entity.SellerProfile;
import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.domain.entity.UserProfile;
import com.eshop.app.seller.shared.domain.enums.SellerIdentityType;
import com.eshop.app.seller.shared.domain.enums.SellerStatus;
import com.eshop.app.seller.application.port.in.RegisterSellerUseCase;
import com.eshop.app.seller.application.port.in.SellerAdminUseCase;
import com.eshop.app.user.domain.entity.Role;
import com.eshop.app.user.domain.repository.SellerProfileRepository;
import com.eshop.app.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.eshop.app.user.application.service.SellerProfileService;
import com.eshop.app.user.application.service.KeycloakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@org.springframework.test.context.TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
public class SellerProfileReproductionTest {

    @Autowired
    private RegisterSellerUseCase sellerRegistrationService;

    @Autowired
    private SellerProfileService sellerProfileService;

    @Autowired
    private SellerAdminUseCase sellerAdminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SellerProfileRepository sellerProfileRepository;

    @MockitoBean
    private KeycloakService keycloakService;

    private User testUser;

    @BeforeEach
    public void setup() {
        // Mock Keycloak calls
        doNothing().when(keycloakService).assignRole(anyString(), anyString());

        // Create a test user with proper back-reference
        testUser = User.builder()
                .keycloakId("test-keycloak-id")
                .email("test.seller@example.com")
                .role(Role.CUSTOMER) // Starts as CUSTOMER
                .build();

        UserProfile profile = UserProfile.builder()
                .user(testUser)
                .firstName("Test")
                .lastName("Seller")
                .phone("9876543210")
                .build();

        testUser.setUserProfile(profile);
        testUser = userRepository.save(testUser);
    }

    @Test
    @WithMockUser(username = "test_seller_repro", roles = { "CUSTOMER" })
    public void testSellerRegistrationAndApprovalFlow() {
        // 1. Register as Seller
        SellerRegisterRequest request = new SellerRegisterRequest();
        request.setIdentityType(SellerIdentityType.INDIVIDUAL);
        request.setBusinessTypes(Set.of(com.eshop.app.seller.shared.domain.enums.SellerBusinessType.FARMER));
        request.setShopName("Test Farm Repro");
        request.setShopHandle("test-farm-repro");
        request.setBusinessName("Test Farm Business");
        request.setDescription("Test Description");
        request.setAcceptedTerms(true);
        request.setFarmLocationVillage("Test Village");
        request.setLandArea("5 Acres");
        request.setIsOwnProduce(true);

        // Verification details
        request.setPanNumber("ABCDE1234F");
        request.setAadhar("123456789012");

        // Use proper address fields as required by validators/processors
        request.setAddressLine1("123 Test St");
        request.setCity("Test City");
        request.setDistrict("Test District");
        request.setState("Test State");
        request.setPincode("123456");
        request.setCountry("India");
        request.setPhone("9876543210");
        request.setBusinessPhone("9876543211");

        // Store/Warehouse address (Physical presence check)
        request.setStoreAddressLine1("456 Warehouse Way");
        request.setStoreCity("Test City");
        request.setStoreDistrict("Test District");
        request.setStoreState("Test State");
        request.setStorePincode("123456");
        request.setStoreCountry("India");

        SellerProfileResponse registerResponse = sellerRegistrationService.registerSeller(testUser.getId(), request);

        assertNotNull(registerResponse);
        assertEquals(SellerStatus.PENDING, registerResponse.getStatus());
        assertEquals(testUser.getId(), registerResponse.getUserId());

        // Verify profile exists in DB
        assertTrue(sellerProfileRepository.existsByUser_Id(testUser.getId()));

        // 2. Approve Seller (Admin action)
        sellerAdminService.approveSeller(registerResponse.getId(), "admin-user");

        // Verify Status is ACTIVE
        SellerProfile approvedProfile = sellerProfileRepository.findById(registerResponse.getId()).orElseThrow();
        assertEquals(SellerStatus.ACTIVE, approvedProfile.getStatus());

        // 3. Retrieve Profile via Service (Simulating controller call)
        SellerProfileResponse fetchedResponse = sellerProfileService.getSellerProfile(testUser.getId());

        assertNotNull(fetchedResponse);
        assertEquals(SellerStatus.ACTIVE, fetchedResponse.getStatus());
        assertEquals("Test Farm Repro", fetchedResponse.getShopName());
    }
}
