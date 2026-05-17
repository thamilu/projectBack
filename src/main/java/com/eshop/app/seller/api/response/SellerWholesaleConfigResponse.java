package com.eshop.app.seller.api.response;





import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerWholesaleConfigResponse {
    private Long id;
    private Boolean bulkPricingEnabled;
    private Integer minOrderQuantity;
}
