package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Shipping class for rate calculation.
 */
@Entity
@Table(name = "shipping_classes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingClass extends BaseEntity {
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "code", unique = true, length = 50)
    private String code;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;
}
