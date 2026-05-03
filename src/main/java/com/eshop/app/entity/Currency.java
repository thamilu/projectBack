package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "currencies", indexes = {
    @Index(name = "idx_currency_code", columnList = "code", unique = true),
    @Index(name = "idx_currency_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Currency extends BaseEntity {
    
    @Column(nullable = false, unique = true, length = 3)
    private String code; // ISO 4217 code (e.g., "USD", "EUR", "INR")
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false, length = 10)
    private String symbol;
    
    @Column(name = "symbol_position", nullable = false, length = 10)
    @Builder.Default
    private String symbolPosition = "LEFT"; // LEFT or RIGHT
    
    @Column(name = "decimal_places", nullable = false)
    @Builder.Default
    private Integer decimalPlaces = 2;
    
    @Column(name = "exchange_rate", nullable = false, precision = 15, scale = 8)
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;
    
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @Column(name = "thousands_separator", length = 5)
    @Builder.Default
    private String thousandsSeparator = ",";
    
    @Column(name = "decimal_separator", length = 5)
    @Builder.Default
    private String decimalSeparator = ".";
}
