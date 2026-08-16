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
 * Response DTO representing an available seller business type.
 * Specifies the classification key and display label.
 *
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Seller business type classification details")
public class SellerBusinessTypeResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Business type code identifier. */
    @Schema(description = "Business type classification identifier. Possible values include: INDIVIDUAL, COMPANY, PARTNERSHIP, LLP, FARMER.",
            example = "INDIVIDUAL")
    private String type;

    /** Display name. */
    @Schema(description = "User-facing display label for the business classification.", example = "Individual")
    private String label;
}
