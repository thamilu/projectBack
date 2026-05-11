package com.eshop.app.seed.seeders;

import com.eshop.app.entity.Cart;
import com.eshop.app.repository.CartRepository;
import com.eshop.app.enums.UserRole;
import com.eshop.app.seed.core.BaseSeeder;
import com.eshop.app.seed.core.SeederContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Cart seeder - Order 7 (final).
 * Creates empty carts for customer users.
 * Depends on UserSeeder.
 */
@Slf4j
@Component
@Order(7)
@RequiredArgsConstructor
public class CartSeeder extends BaseSeeder<Cart, SeederContext> {
    
    private final CartRepository cartRepository;
    
    @Override
    protected List<Cart> doSeed(SeederContext context) {
        List<Cart> carts = new ArrayList<>();
        
        // Create cart for each customer
        context.getUsers().values().stream()
                .filter(user -> user.getRole() == UserRole.CUSTOMER)
            .forEach(customer -> {
                Cart cart = Cart.builder()
                    .user(customer)
                    .build();
                carts.add(cart);
            });
        
        if (!carts.isEmpty()) {
            return cartRepository.saveAll(carts);
        }
        
        return List.of();
    }
    
    @Override
    protected void doCleanup() {
        cartRepository.deleteAllInBatch();
    }
    
    @Override
    public int order() {
        return 7;
    }
}
