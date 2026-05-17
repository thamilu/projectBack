package com.eshop.app.seller.api.response;





import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerBusinessTypeResponse {
    private String type;
    private String label;
}
