package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tax_rates", indexes = {
    @Index(name = "idx_tax_rate_geo_zone", columnList = "geo_zone_id"),
    @Index(name = "idx_tax_rate_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxRate extends BaseEntity {
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal rate; // Tax rate (e.g., 18.00 for 18%)
    
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TaxType type = TaxType.PERCENTAGE;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geo_zone_id", nullable = false)
    private GeoZone geoZone;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean compound = false; // Compound tax calculation
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    public enum TaxType {
        PERCENTAGE,
        FIXED
    }
}
