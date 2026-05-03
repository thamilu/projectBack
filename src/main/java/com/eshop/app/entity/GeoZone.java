package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "geo_zones", indexes = {
    @Index(name = "idx_geo_zone_country", columnList = "country"),
    @Index(name = "idx_geo_zone_state", columnList = "state")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeoZone extends BaseEntity {
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false, length = 2)
    private String country; // ISO 3166-1 alpha-2 code
    
    @Column(length = 100)
    private String state;
    
    @Column(length = 100)
    private String city;
    
    @Column(name = "zip_code_pattern", length = 255)
    private String zipCodePattern; // Regex pattern for zip codes
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
