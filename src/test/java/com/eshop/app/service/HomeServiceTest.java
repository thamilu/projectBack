package com.eshop.app.service;

import com.eshop.app.dto.response.HomeResponse;
import com.eshop.app.entity.SellerProfile;
import com.eshop.app.entity.Store;
import com.eshop.app.entity.User;
import com.eshop.app.entity.UserProfile;
import com.eshop.app.enums.UserRole;
import com.eshop.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private HomeService homeService;

    private User adminUser;
    private User sellerUser;
    private User customerUser;
    private User deliveryAgentUser;

    @BeforeEach
    void setUp() {
        // Admin User
        adminUser = User.builder()
                .keycloakId("admin-uuid")
                .role(UserRole.ADMIN)
                .build();
        adminUser.setUserProfile(UserProfile.builder().firstName("Admin").lastName("User").user(adminUser).build());

        // Seller User
        sellerUser = User.builder()
                .keycloakId("seller-uuid")
                .role(UserRole.SELLER)
                .build();
        sellerUser.setUserProfile(UserProfile.builder().firstName("Seller").lastName("User").user(sellerUser).build());

        SellerProfile sellerProfile = SellerProfile.builder()
                .user(sellerUser)
                .build();
        Store store = Store.builder()
                .storeName("Test Store")
                .active(true)
                .sellerProfile(sellerProfile)
                .build();
        sellerProfile.setStores(java.util.Set.of(store));
        sellerUser.setSellerProfile(sellerProfile);

        // Customer User
        customerUser = User.builder()
                .keycloakId("customer-uuid")
                .role(UserRole.CUSTOMER)
                .build();
        customerUser.setUserProfile(
                UserProfile.builder().firstName("Customer").lastName("User").user(customerUser).build());

        // Delivery Agent User
        deliveryAgentUser = User.builder()
                .keycloakId("delivery-uuid")
                .role(UserRole.DELIVERY_AGENT)
                .build();
        deliveryAgentUser.setUserProfile(
                UserProfile.builder().firstName("Delivery").lastName("Agent").user(deliveryAgentUser).build());
    }

    @Test
    void testGetHomePageData_GuestUser() {
        // When
        HomeResponse response = homeService.getHomePageDataForUser(null);

        // Then
        assertNotNull(response);
        assertEquals("GUEST", response.getUserRole());
        assertEquals("Guest User", response.getUserName());
        assertEquals("Welcome to EShop!", response.getMessage());
        assertTrue(response.getAvailableActions().contains("Browse Products"));
        assertTrue(response.getAvailableActions().contains("Register Account"));
        assertTrue(response.getAvailableActions().contains("Login"));
    }

    @Test
    void testGetHomePageData_AdminUser() {
        // When
        HomeResponse response = homeService.getHomePageDataForUser(adminUser);

        // Then
        assertNotNull(response);
        assertEquals("ADMIN", response.getUserRole());
        assertEquals("Admin User", response.getUserName());
        assertEquals("Welcome to Admin Dashboard", response.getMessage());
        assertTrue(response.getAvailableActions().contains("Manage Users"));
        assertTrue(response.getAvailableActions().contains("Manage Products"));
        assertTrue(response.getAvailableActions().contains("Manage Stores"));
        assertTrue(response.getAvailableActions().contains("View Analytics"));
        assertTrue(response.getAvailableActions().contains("System Settings"));
        assertTrue(response.getQuickLinks().containsKey("users"));
        assertTrue(response.getQuickLinks().containsKey("products"));
        assertTrue(response.getQuickLinks().containsKey("stores"));
    }

    @Test
    void testGetHomePageData_SellerUser() {
        // When
        HomeResponse response = homeService.getHomePageDataForUser(sellerUser);

        // Then
        assertNotNull(response);
        assertEquals("SELLER", response.getUserRole());
        assertEquals("Seller User", response.getUserName());
        assertEquals("Welcome to Seller Dashboard", response.getMessage());
        assertTrue(response.getAvailableActions().contains("Manage Products"));
        assertTrue(response.getAvailableActions().contains("View Orders"));
        assertTrue(response.getAvailableActions().contains("Store Settings"));
        assertTrue(response.getAvailableActions().contains("Sales Analytics"));
        assertTrue(response.getAvailableActions().contains("Customer Reviews"));
        assertTrue(response.getQuickLinks().containsKey("myProducts"));
        assertTrue(response.getQuickLinks().containsKey("orders"));
        assertTrue(response.getQuickLinks().containsKey("store")); // Fixed: "store" not "shop"
    }

    @Test
    void testGetHomePageData_CustomerUser() {
        // When
        HomeResponse response = homeService.getHomePageDataForUser(customerUser);

        // Then
        assertNotNull(response);
        assertEquals("CUSTOMER", response.getUserRole());
        assertEquals("Customer User", response.getUserName());
        assertEquals("Welcome back!", response.getMessage());
        assertTrue(response.getAvailableActions().contains("Browse Products"));
        assertTrue(response.getAvailableActions().contains("View Cart"));
        assertTrue(response.getAvailableActions().contains("My Orders"));
        assertTrue(response.getAvailableActions().contains("Account Settings"));
        assertTrue(response.getAvailableActions().contains("Track Deliveries"));
        assertTrue(response.getQuickLinks().containsKey("products"));
        assertTrue(response.getQuickLinks().containsKey("cart"));
        assertTrue(response.getQuickLinks().containsKey("orders"));
    }

    @Test
    void testGetHomePageData_DeliveryAgentUser() {
        // When
        HomeResponse response = homeService.getHomePageDataForUser(deliveryAgentUser);

        // Then
        assertNotNull(response);
        assertEquals("DELIVERY_AGENT", response.getUserRole());
        assertEquals("Delivery Agent", response.getUserName());
        assertEquals("Delivery Agent Dashboard", response.getMessage());
        assertTrue(response.getAvailableActions().contains("View Assigned Deliveries"));
        assertTrue(response.getAvailableActions().contains("Update Delivery Status"));
        assertTrue(response.getAvailableActions().contains("Delivery History"));
        assertTrue(response.getAvailableActions().contains("Performance Metrics"));
        assertTrue(response.getAvailableActions().contains("Route Planning"));
        assertTrue(response.getQuickLinks().containsKey("deliveries"));
        assertTrue(response.getQuickLinks().containsKey("updateStatus"));
        assertTrue(response.getQuickLinks().containsKey("history"));
    }

    @Test
    void testAllRoleSpecificDashboardsHaveRequiredFields() {
        // Test all roles have required fields
        User[] users = {adminUser, sellerUser, customerUser, deliveryAgentUser};
        
        for (User user : users) {
            HomeResponse response = homeService.getHomePageDataForUser(user);
            
            assertNotNull(response.getMessage(), "Message should not be null for " + user.getRole());
            assertNotNull(response.getUserRole(), "User role should not be null");
            assertNotNull(response.getUserName(), "User name should not be null");
            assertNotNull(response.getDashboardData(), "Dashboard data should not be null");
            assertNotNull(response.getAvailableActions(), "Available actions should not be null");
            assertNotNull(response.getQuickLinks(), "Quick links should not be null");
            assertNotNull(response.getTimestamp(), "Timestamp should not be null");
            
            assertFalse(response.getAvailableActions().isEmpty(), 
                "Available actions should not be empty for " + user.getRole());
            assertFalse(response.getQuickLinks().isEmpty(), 
                "Quick links should not be empty for " + user.getRole());
        }
    }
}