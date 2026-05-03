package com.eshop.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationPricingDto {
    private String countryCode;
    private String currency;
    private BigDecimal basePrice;
    private BigDecimal salePrice;
    private Double taxRate;
}
