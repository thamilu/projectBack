package com.eshop.app.seller.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class RejectionRequest {
    @NotBlank(message = "Rejection reason is required")
    @jakarta.validation.constraints.Size(min = 10, max = 500, message = "Rejection reason must be between 10 and 500 characters")
    @jakarta.validation.constraints.Pattern(regexp = "^(?=.*[a-zA-Z])[\\s\\S]*$", message = "Rejection reason contains invalid characters or does not contain any letters")
    private String reason;
}
