package com.eshop.app.seller.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Response DTO representing seller wholesale configuration. */
@Getter
@lombok.Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Seller wholesale configuration")
public class SellerWholesaleConfigResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Wholesale configuration identifier", example = "1")
    private Long id;

    @Schema(description = "Whether bulk pricing is enabled", example = "true")
    private Boolean bulkPricingEnabled;

    @Schema(description = "Minimum order quantity for wholesale pricing", example = "10")
    private Integer minOrderQuantity;
}
