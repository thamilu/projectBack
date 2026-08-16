package com.eshop.app.customer.api.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistAddRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;
}
