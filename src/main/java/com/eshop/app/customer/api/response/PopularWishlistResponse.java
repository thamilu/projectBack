package com.eshop.app.customer.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularWishlistResponse {
    private Long productId;
    private String productName;
    private Long wishlistCount;
}
