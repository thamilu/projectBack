package com.eshop.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryZoneDto {
    private Long zoneId;
    private String zoneName;
    private String type;
    private Integer radiusKm;
    private java.util.Map<String, Integer> deliveryDays; // {min, max}
    private java.math.BigDecimal deliveryFee;
    private java.math.BigDecimal freeDeliveryAbove;
    private java.util.List<String> countries;
}
