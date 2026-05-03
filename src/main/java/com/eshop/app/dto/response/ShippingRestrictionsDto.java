package com.eshop.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingRestrictionsDto {
    private Boolean hazardousMaterial;
    private Boolean requiresSignature;
    private Boolean temperatureControlled;
    private Boolean fragile;
    private Boolean oversized;
    private Boolean internationalShipping;
    private List<String> excludedCarriers;
    private List<String> allowedCarriers;
}
