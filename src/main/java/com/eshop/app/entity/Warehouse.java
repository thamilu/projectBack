package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Warehouse entity for multi-warehouse inventory management.
 */
@Entity
@Table(name = "warehouses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse extends BaseEntity {
    
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    
    @Column(name = "code", unique = true, length = 50)
    private String code;
    
    @Column(name = "address", length = 500)
    private String address;
    
    @Column(name = "city", length = 100)
    private String city;
    
    @Column(name = "state", length = 100)
    private String state;
    
    @Column(name = "country", length = 100)
    private String country;
    
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    
    @Column(name = "phone", length = 50)
    private String phone;
    
    @Column(name = "email", length = 100)
    private String email;
    
    @Column(name = "manager_name", length = 200)
    private String managerName;
    
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;
    
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @Column(name = "priority")
    private Integer priority;
}
