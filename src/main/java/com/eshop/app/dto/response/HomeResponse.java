package com.eshop.app.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import com.eshop.app.constants.ApiConstants;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class HomeResponse {
    private String message;
    private String userRole;
    private String userName;
    private Map<String, Object> dashboardData;
    private List<String> availableActions;
    private Map<String, String> quickLinks;
    private LocalDateTime timestamp;
    
    public static HomeResponse forGuest() {
        return HomeResponse.builder()
                .message("Welcome to EShop!")
                .userRole("GUEST")
                .userName("Guest User")
                .dashboardData(Map.of(
                        "totalProducts", "Browse our catalog",
                        "featuredCategories", "Discover trending items"
                ))
                .availableActions(List.of(
                        "Browse Products",
                        "Register Account",
                        "Login"
                ))
                .quickLinks(Map.of(
                        "products", ApiConstants.Endpoints.PRODUCTS,
                        "brands", ApiConstants.Endpoints.BRANDS,
                        "categories", ApiConstants.Endpoints.CATEGORIES,
                        "stores", ApiConstants.Endpoints.STORES
                ))
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static HomeResponse forAdmin(String userName, Map<String, Object> adminData) {
        return HomeResponse.builder()
                .message("Welcome to Admin Dashboard")
                .userRole("ADMIN")
                .userName(userName)
                .dashboardData(adminData)
                .availableActions(List.of(
                        "Manage Users",
                        "Manage Products", 
                        "Manage Stores",
                        "View Analytics",
                        "System Settings"
                ))
                .quickLinks(Map.of(
                        "users", ApiConstants.BASE_PATH + "/admin/users",
                        "products", ApiConstants.BASE_PATH + "/admin/products",
                        "stores", ApiConstants.BASE_PATH + "/admin/stores",
                        "analytics", ApiConstants.BASE_PATH + "/admin/analytics"
                ))
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static HomeResponse forSeller(String userName, Map<String, Object> sellerData) {
        return HomeResponse.builder()
                .message("Welcome to Seller Dashboard")
                .userRole("SELLER")
                .userName(userName)
                .dashboardData(sellerData)
                .availableActions(List.of(
                        "Manage Products",
                        "View Orders",
                        "Store Settings", 
                        "Sales Analytics",
                        "Customer Reviews"
                ))
                .quickLinks(Map.of(
                        "myProducts", ApiConstants.BASE_PATH + "/seller/products",
                        "orders", ApiConstants.BASE_PATH + "/seller/orders",
                        "store", ApiConstants.BASE_PATH + "/seller/store",
                        "analytics", ApiConstants.BASE_PATH + "/seller/analytics"
                ))
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static HomeResponse forCustomer(String userName, Map<String, Object> customerData) {
        return HomeResponse.builder()
                .message("Welcome back!")
                .userRole("CUSTOMER")
                .userName(userName)
                .dashboardData(customerData)
                .availableActions(List.of(
                        "Browse Products",
                        "View Cart",
                        "My Orders",
                        "Account Settings",
                        "Track Deliveries"
                ))
                .quickLinks(Map.of(
                        "products", ApiConstants.Endpoints.PRODUCTS,
                        "cart", ApiConstants.Endpoints.CART,
                        "orders", ApiConstants.Endpoints.ORDERS,
                        "profile", ApiConstants.BASE_PATH + "/users/profile"
                ))
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static HomeResponse forDeliveryAgent(String userName, Map<String, Object> deliveryData) {
        return HomeResponse.builder()
                .message("Delivery Agent Dashboard")
                .userRole("DELIVERY_AGENT")
                .userName(userName)
                .dashboardData(deliveryData)
                .availableActions(List.of(
                        "View Assigned Deliveries",
                        "Update Delivery Status",
                        "Delivery History",
                        "Performance Metrics",
                        "Route Planning"
                ))
                .quickLinks(Map.of(
                        "deliveries", ApiConstants.BASE_PATH + "/delivery/assignments",
                        "updateStatus", ApiConstants.BASE_PATH + "/delivery/status",
                        "history", ApiConstants.BASE_PATH + "/delivery/history",
                        "profile", ApiConstants.BASE_PATH + "/delivery/profile"
                ))
                .timestamp(LocalDateTime.now())
                .build();
    }
}