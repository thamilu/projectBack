package com.eshop.app.seller.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO representing farmer-specific verification details associated with a seller account.
 *
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Seller farmer details information")
public class SellerFarmerDetailsResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Farmer details record ID. */
    @Schema(description = "Farmer details record ID", example = "1")
    private Long id;

    /** Whether the seller grows the products being sold. */
    @Schema(description = "Whether the seller grows the products being sold. " +
            "Values: true = seller grows the products directly, false = seller resells products grown by others.",
            example = "true")
    private Boolean isOwnProduce;

    /** Farm location. */
    @Schema(description = "Location description of the farm.", example = "Mysuru, Karnataka")
    private String farmLocation;

    /** Cultivated land area including unit. */
    @Schema(description = "Cultivated land area, specified with its measurement unit.", example = "5 Acres")
    private String landArea;

    /** Cultivated crop types. */
    @Schema(description = "Comma-separated list of cultivated crop types.", example = "Rice, Sugarcane, Coconut")
    private String cropTypes;
}
