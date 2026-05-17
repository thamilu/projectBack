package com.eshop.app.seller.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectionRequest {
    @NotBlank(message = "Rejection reason is required")
    private String reason;
}
