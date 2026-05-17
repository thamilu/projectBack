package com.eshop.app.order.application.mapper;

import com.eshop.app.catalog.domain.entity.Product;
import com.eshop.app.order.api.response.OrderItemResponse;
import com.eshop.app.order.api.response.OrderResponse;
import com.eshop.app.order.domain.entity.Order;
import com.eshop.app.order.domain.entity.OrderItem;
import com.eshop.app.user.domain.entity.User;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderItemResponse toOrderItemResponse(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }
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
        if (order == null) {
            return null;
        }
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        if (order.getCustomer() != null) {
            response.setCustomerId(order.getCustomer().getId());
            User customer = order.getCustomer();
            if (customer.getUserProfile() != null) {
                response.setCustomerName(
                        (customer.getUserProfile().getFirstName() != null ? customer.getUserProfile().getFirstName() : "")
                                + " "
                                + (customer.getUserProfile().getLastName() != null ? customer.getUserProfile().getLastName() : ""));
            } else {
                response.setCustomerName(customer.getEmail());
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
                                + (agent.getUserProfile().getLastName() != null ? agent.getUserProfile().getLastName() : ""));
            } else {
                response.setDeliveryAgentName(agent.getEmail());
            }
        }

        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

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
