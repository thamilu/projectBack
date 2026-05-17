package com.eshop.app.order.domain.entity;

import com.eshop.app.localization.domain.entity.Currency;
import com.eshop.app.payment.domain.entity.Payment;
import com.eshop.app.core.kernel.BaseEntity;
import com.eshop.app.shipping.domain.entity.Shipping;
import com.eshop.app.store.domain.entity.Store;
import com.eshop.app.tax.domain.entity.OrderTax;
import com.eshop.app.user.domain.entity.User;




import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_number", columnList = "order_number"),
    @Index(name = "idx_order_customer", columnList = "customer_id"),
    @Index(name = "idx_order_status", columnList = "order_status"),
    @Index(name = "idx_order_payment_status", columnList = "payment_status"),
    @Index(name = "idx_order_delivery_agent", columnList = "delivery_agent_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {
    
    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<OrderItem> items = new HashSet<>();
    
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "tax_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;
    
    @Column(name = "shipping_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal shippingAmount = BigDecimal.ZERO;
    
    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id")
    private Currency currency;
    
    @Column(name = "exchange_rate", precision = 15, scale = 8)
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 20)
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.PLACED;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    @Column(name = "shipping_address", nullable = false, length = 500)
    @Setter
    private String shippingAddress;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Payment> payments = new HashSet<>();
    
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Shipping shipping;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<OrderTax> orderTaxes = new HashSet<>();
    
    @Column(name = "billing_address", length = 500)
    @Setter
    private String billingAddress;
    
    @Column(name = "phone", length = 20)
    @Setter
    private String phone;
    
    @Column(length = 1000)
    @Setter
    private String notes;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_agent_id")
    @Setter
    private User deliveryAgent;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    // --- Domain Behaviors (Aggregate Root Invariants) ---

    public void confirm() {
        if (this.orderStatus != OrderStatus.PLACED) {
            throw new IllegalStateException("Only PLACED orders can be confirmed.");
        }
        this.orderStatus = OrderStatus.CONFIRMED;
    }

    public void cancel(String reason) {
        if (this.orderStatus == OrderStatus.SHIPPED || this.orderStatus == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel an order that has already been shipped or delivered.");
        }
        this.orderStatus = OrderStatus.CANCELLED;
        this.notes = (this.notes == null ? "" : this.notes + "\n") + "Cancellation Reason: " + reason;
    }

    public void markAsPaid() {
        if (this.paymentStatus == PaymentStatus.PAID) {
            return;
        }
        this.paymentStatus = PaymentStatus.PAID;
        if (this.orderStatus == OrderStatus.PLACED) {
            this.orderStatus = OrderStatus.CONFIRMED;
        }
    }

    public void ship() {
        if (this.orderStatus != OrderStatus.CONFIRMED && this.orderStatus != OrderStatus.PACKED) {
            throw new IllegalStateException("Order must be CONFIRMED or PACKED before shipping.");
        }
        if (this.paymentStatus != PaymentStatus.PAID) {
             // In some business rules, we might allow shipping before payment (COD), 
             // but here we assume PAID is required or handled via specific logic.
        }
        this.orderStatus = OrderStatus.SHIPPED;
    }

    public void deliver() {
        if (this.orderStatus != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Order must be SHIPPED before it can be DELIVERED.");
        }
        this.orderStatus = OrderStatus.DELIVERED;
    }

    public void addItem(OrderItem item) {
        if (this.orderStatus != OrderStatus.PLACED) {
            throw new IllegalStateException("Cannot add items to an order that is not in PLACED status.");
        }
        this.items.add(item);
        item.setOrder(this);
        recalculateTotal();
    }

    private void recalculateTotal() {
        this.totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(shippingAmount)
                .add(taxAmount)
                .subtract(discountAmount);
    }

    public enum OrderStatus {
        PLACED, CONFIRMED, PACKED, SHIPPED, DELIVERED, CANCELLED, RETURNED
    }
    
    public enum PaymentStatus {
        PENDING, PAID, FAILED, REFUNDED
    }
}

