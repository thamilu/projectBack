package com.eshop.app.service;

import com.eshop.app.dto.request.SellerRegisterRequest;
import com.eshop.app.dto.response.SellerProfileResponse;
import com.eshop.app.entity.SellerProfile;
import com.eshop.app.entity.User;
import com.eshop.app.entity.UserProfile;
import com.eshop.app.enums.SellerIdentityType;
import com.eshop.app.enums.SellerStatus;
import com.eshop.app.enums.UserRole;
import com.eshop.app.repository.SellerProfileRepository;
import com.eshop.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    private SellerService sellerService;

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

        // Create a test user
        testUser = User.builder()
                .keycloakId("test-keycloak-id")
                .role(UserRole.CUSTOMER) // Starts as CUSTOMER
                .userProfile(UserProfile.builder().firstName("Test").lastName("Seller").phone("9876543210").build())
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    @WithMockUser(username = "test_seller_repro", roles = { "CUSTOMER" })
    public void testSellerRegistrationAndApprovalFlow() {
        // 1. Register as Seller
        SellerRegisterRequest request = new SellerRegisterRequest();
        request.setIdentityType(SellerIdentityType.INDIVIDUAL);
        request.setBusinessTypes(Set.of(com.eshop.app.enums.SellerBusinessType.FARMER));
        request.setShopName("Test Farm Repro");
        request.setBusinessName("Test Farm Business"); // Required for some logic potentially?
        request.setDescription("Test Description");
        request.setAcceptedTerms(true);
        request.setFarmLocationVillage("Test Village");
        request.setLandArea("5 Acres");

        SellerProfileResponse registerResponse = sellerService.registerSeller(testUser.getId(), request);

        assertNotNull(registerResponse);
        assertEquals(SellerStatus.PENDING, registerResponse.getStatus());
        assertEquals(testUser.getId(), registerResponse.getUserId());

        // Verify profile exists in DB
        assertTrue(sellerProfileRepository.existsByUser_Id(testUser.getId()));

        // 2. Approve Seller (Admin action)
        sellerService.approveSeller(registerResponse.getId(), "admin-user");

        // Verify Status is ACTIVE
        SellerProfile approvedProfile = sellerProfileRepository.findById(registerResponse.getId()).orElseThrow();
        assertEquals(SellerStatus.ACTIVE, approvedProfile.getStatus());

        // 3. Retrieve Profile via Service (Simulating controller call)
        // Note: In real flow, the user role might update in Keycloak but here we mock
        // it/or assert DB state
        SellerProfileResponse fetchedResponse = sellerService.getSellerProfile(testUser.getId());

        assertNotNull(fetchedResponse);
        assertEquals(SellerStatus.ACTIVE, fetchedResponse.getStatus());
        assertEquals("Test Farm Repro", fetchedResponse.getShopName());
    }
}
