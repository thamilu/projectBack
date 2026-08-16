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
 * Response DTO representing registered seller business details.
 * Holds business registration identity data, legal naming representations,
 * and key logistics warehouse coordinates.
 *
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Seller business profile details")
public class SellerBusinessDetailsResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Unique details record ID. */
    @Schema(description = "Business details record ID", example = "1")
    private Long id;

    /** Registered legal business name. */
    @Schema(description = "Registered legal business entity name.", example = "ABC Traders Private Limited")
    private String legalBusinessName;

    /** Full legal name of the authorized business signatory. */
    @Schema(description = "Full legal name of the authorized business signatory.", example = "Ravi Kumar")
    private String authorizedSignatory;

    /** Logistics warehouse location name or address. */
    @Schema(description = "Primary warehouse location description or address details.", example = "Bengaluru, Karnataka")
    private String warehouseLocation;
}
