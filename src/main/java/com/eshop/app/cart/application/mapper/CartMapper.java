package com.eshop.app.cart.application.mapper;

import com.eshop.app.cart.api.response.CartItemResponse;
import com.eshop.app.cart.api.response.CartResponse;
import com.eshop.app.cart.domain.entity.Cart;
import com.eshop.app.cart.domain.entity.CartItem;
import com.eshop.app.catalog.domain.entity.Product;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CartMapper {

    public CartResponse toCartResponse(Cart cart) {
        if (cart == null) {
            return null;
        }
        CartResponse response = new CartResponse();
        response.setId(cart.getId());
        if (cart.getUser() != null) {
            response.setUserId(cart.getUser().getId());
        }

        if (cart.getItems() != null && !cart.getItems().isEmpty()) {
            List<CartItemResponse> items = cart.getItems().stream()
                .map(this::toCartItemResponse)
                .collect(Collectors.toList());
            response.setItems(items);
            response.setTotalItems(items.size());
        } else {
            response.setItems(List.of());
            response.setTotalItems(0);
        }

        response.setTotalAmount(cart.getTotalAmount());
        return response;
    }

    public CartItemResponse toCartItemResponse(CartItem cartItem) {
        if (cartItem == null) {
            return null;
        }
        CartItemResponse response = new CartItemResponse();
        response.setId(cartItem.getId());
        if (cartItem.getProduct() != null) {
            response.setProductId(cartItem.getProduct().getId());
            response.setProductName(cartItem.getProduct().getName());
            response.setProductImage(getProductImageUrl(cartItem.getProduct()));
        }
        response.setQuantity(cartItem.getQuantity());
        response.setPrice(cartItem.getPrice());
        if (cartItem.getPrice() != null && cartItem.getQuantity() != null) {
            response.setSubtotal(cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        return response;
    }

    private String getProductImageUrl(Product product) {
        if (product == null) {
            return null;
        }
        if (product.getPrimaryImage() != null) {
            return product.getPrimaryImage().getUrl();
        }
        try {
            if (product.getImages() != null && Hibernate.isInitialized(product.getImages()) && !product.getImages().isEmpty()) {
                com.eshop.app.catalog.domain.entity.ProductImage img = product.getImages().get(0);
                if (img != null && img.getUrl() != null) {
                    return img.getUrl();
                }
            }
        } catch (Exception e) {
            // ignore and return null
        }
        return null;
    }
}
