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
 * Response DTO representing an available seller identity document type.
 * Specifies the code key, client-facing display label, and functional description.
 *
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Seller identity document type specification")
public class SellerIdentityTypeResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Identity document type code key. */
    @Schema(description = "Unique code key representing the document type.", example = "PAN")
    private String type;

    /** Display label. */
    @Schema(description = "User-friendly display label of the document type.", example = "PAN Card")
    private String label;

    /** Description of the identity document. */
    @Schema(description = "Functional description of the document type including validation boundaries.",
            example = "Permanent Account Number issued by the Income Tax Department.")
    private String description;
}
