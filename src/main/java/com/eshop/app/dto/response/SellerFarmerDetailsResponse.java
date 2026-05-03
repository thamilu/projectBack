package com.eshop.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerFarmerDetailsResponse {
    private Long id;
    private Boolean isOwnProduce;
    private String farmLocation;
    private String landArea;
    private String cropTypes;
}
