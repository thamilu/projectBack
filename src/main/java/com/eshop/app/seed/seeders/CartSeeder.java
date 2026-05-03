package com.eshop.app.seed.seeders;

import com.eshop.app.entity.Cart;
import com.eshop.app.entity.User;
import com.eshop.app.repository.CartRepository;
import com.eshop.app.enums.UserRole;
import com.eshop.app.seed.core.Seeder;
import com.eshop.app.seed.core.SeederContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cart seeder - Order 7 (final).
 * Creates empty carts for customer users.
 * Depends on UserSeeder.
 */
@Slf4j
@Component
@Order(7)
@RequiredArgsConstructor
public class CartSeeder implements Seeder<Cart, SeederContext> {
    
    private final CartRepository cartRepository;
    
    @Override
    public List<Cart> seed(SeederContext context) {
        Map<String, User> users = context.getUsers();
        List<Cart> carts = new ArrayList<>();
        
        // Create cart for each customer
        users.values().stream()
                .filter(user -> user.getRole() == UserRole.CUSTOMER)
            .forEach(customer -> {
                Cart cart = Cart.builder()
                    .user(customer)
                    .build();
                carts.add(cart);
            });
        
        if (!carts.isEmpty()) {
            List<Cart> savedCarts = cartRepository.saveAll(carts);
            log.info("Seeded {} carts successfully", savedCarts.size());
            return savedCarts;
        }
        
        log.info("No customer users found, skipping cart seeding");
        return List.of();
    }
    
    @Override
    public void cleanup() {
        try {
            cartRepository.deleteAllInBatch();
            log.debug("Cleaned up existing carts");
        } catch (Exception e) {
            log.warn("Failed to cleanup carts: {}", e.getMessage());
        }
    }
    
    @Override
    public int order() {
        return 7;
    }
    
    @Override
    public String name() {
        return "CartSeeder";
    }
}
