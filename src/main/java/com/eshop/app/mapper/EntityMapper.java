package com.eshop.app.mapper;

import com.eshop.app.dto.response.*;
import com.eshop.app.entity.*;
import org.hibernate.Hibernate;
// import org.modelmapper.ModelMapper; // Removed for MapStruct migration
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EntityMapper {

    // Cart mapping
    public CartResponse toCartResponse(Cart cart) {
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

    // Order mappings
    public OrderItemResponse toOrderItemResponse(OrderItem orderItem) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(orderItem.getId());
        if (orderItem.getProduct() != null) {
            response.setProductId(orderItem.getProduct().getId());
            response.setProductName(orderItem.getProduct().getName());
            response.setProductImage(getProductImageUrl(orderItem.getProduct()));
        }
        response.setQuantity(orderItem.getQuantity());
        response.setPrice(orderItem.getPrice());
        response.setDiscountAmount(orderItem.getDiscountAmount());
        response.setSubtotal(orderItem.getSubtotal());
        return response;
    }

    public OrderResponse toOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        if (order.getCustomer() != null) {
            response.setCustomerId(order.getCustomer().getId());
            User customer = order.getCustomer();
            if (customer.getUserProfile() != null) {
                response.setCustomerName(
                        (customer.getUserProfile().getFirstName() != null ? customer.getUserProfile().getFirstName()
                                : "")
                                + " "
                                + (customer.getUserProfile().getLastName() != null
                                        ? customer.getUserProfile().getLastName()
                                        : ""));
            } else {
                response.setCustomerName(customer.getUsername());
            }
            response.setCustomerEmail(order.getCustomer().getEmail());
        }

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            List<OrderItemResponse> items = order.getItems().stream()
                .map(this::toOrderItemResponse)
                .collect(Collectors.toList());
            response.setItems(items);
        } else {
            response.setItems(List.of());
        }

        response.setTotalAmount(order.getTotalAmount());
        response.setTaxAmount(order.getTaxAmount());
        response.setShippingAmount(order.getShippingAmount());
        response.setDiscountAmount(order.getDiscountAmount());
        if (order.getOrderStatus() != null) response.setOrderStatus(order.getOrderStatus().name());
        if (order.getPaymentStatus() != null) response.setPaymentStatus(order.getPaymentStatus().name());
        response.setShippingAddress(order.getShippingAddress());
        response.setBillingAddress(order.getBillingAddress());
        response.setPhone(order.getPhone());
        response.setNotes(order.getNotes());

        if (order.getDeliveryAgent() != null) {
            response.setDeliveryAgentId(order.getDeliveryAgent().getId());
            User agent = order.getDeliveryAgent();
            if (agent.getUserProfile() != null) {
                response.setDeliveryAgentName(
                        (agent.getUserProfile().getFirstName() != null ? agent.getUserProfile().getFirstName() : "")
                                + " "
                                + (agent.getUserProfile().getLastName() != null ? agent.getUserProfile().getLastName()
                                        : ""));
            } else {
                response.setDeliveryAgentName(agent.getUsername());
            }
        }

        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        return response;
    }

    // Product Image mappings
    public ProductImageResponse toProductImageResponse(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .productId(image.getProduct() != null ? image.getProduct().getId() : null)
                .imageUrl(image.getUrl())
                .altText(image.getAltText())
                .isPrimary(image.getIsPrimary())
                .displayOrder(image.getSortOrder())
                .provider(image.getProvider())
                .publicId(image.getPublicId())
                .thumbnailUrl(image.getThumbnailUrl())
                .width(image.getWidth())
                .height(image.getHeight())
                .fileSize(image.getFileSize())
                .createdAt(image.getCreatedAt())
                .updatedAt(image.getUpdatedAt())
                .build();
    }

    // Page mappings
    public <E, D> PageResponse<D> toPageResponse(Page<E> page, List<D> content) {
        PageResponse.PageMetadata metadata = PageResponse.PageMetadata.builder()
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .hasNext(page.hasNext())
            .hasPrevious(page.hasPrevious())
            .build();
        return PageResponse.<D>builder()
            .data(content)
            .pagination(metadata)
            .build();
    }

    // Helper method to get product image URL using non-deprecated methods
    private String getProductImageUrl(Product product) {
        if (product == null) {
            return null;
        }
        // Use primary image if available
        if (product.getPrimaryImage() != null) {
            return product.getPrimaryImage().getUrl();
        }
        // Safely check images collection initialization to avoid LazyInitializationException
        try {
            if (product.getImages() != null && Hibernate.isInitialized(product.getImages()) && !product.getImages().isEmpty()) {
                com.eshop.app.entity.ProductImage img = product.getImages().get(0);
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
