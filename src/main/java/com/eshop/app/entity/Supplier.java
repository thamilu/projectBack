package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Supplier entity for product sourcing.
 */
@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier extends BaseEntity {
    
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    
    @Column(name = "code", unique = true, length = 50)
    private String code;
    
    @Column(name = "contact_name", length = 200)
    private String contactName;
    
    @Column(name = "email", length = 100)
    private String email;
    
    @Column(name = "phone", length = 50)
    private String phone;
    
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
    
    @Column(name = "website", length = 200)
    private String website;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;
}
