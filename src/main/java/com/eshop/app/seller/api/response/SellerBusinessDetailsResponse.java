package com.eshop.app.seller.api.response;





import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerBusinessDetailsResponse {
    private Long id;
    private String legalBusinessName;
    private String authorizedSignatory;
    private String warehouseLocation;
}
