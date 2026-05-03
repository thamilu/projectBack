package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "stores", indexes = {
    @Index(name = "idx_store_name", columnList = "store_name"),
        @Index(name = "idx_store_seller_profile", columnList = "seller_profile_id"),
            @Index(name = "idx_store_location", columnList = "latitude,longitude"),
    @Index(name = "idx_store_city", columnList = "city"),
    @Index(name = "idx_store_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "products")
@EqualsAndHashCode(callSuper = true, exclude = "products")
public class Store extends BaseEntity {
    
    @Column(name = "store_name", nullable = false, length = 200)
    private String storeName;
    
    @Column(nullable = false, length = 1000)
    private String description;
    
    @Column(name = "address_line1", length = 500)
    private String addressLine1;

    @Column(name = "address_line2", length = 500)
    private String addressLine2;
    
    @Column(length = 20)
    private String phone;
    
    @Column(length = 150)
    private String email;
    
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(length = 200)
    private String domain;
    
    // Geospatial fields for location-based search
    @Column
    private Double latitude;
    
    @Column
    private Double longitude;
    
    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String district;
    
    @Column(length = 100)
    private String state;
    
    @Column(length = 100)
    private String country;
    
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    
    @Column(name = "place_id", length = 200)
    private String placeId; // Google Place ID
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @Column
    private Double rating; // Store rating (0-5)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_profile_id", nullable = false)
    private SellerProfile sellerProfile;
    
    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private final Set<Product> products = new HashSet<>();

    @Column(name = "google_maps_url", length = 500)
    private String googleMapsUrl;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;
}