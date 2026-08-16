package com.eshop.app.seller.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO representing a single business type available for seller registration.
 *
 * <p>Replaces the weakly typed {@code Map<String, String>} previously returned by {@code
 * SellerAdminUseCase.getBusinessTypes()}. Provides compile-time safety, OpenAPI documentation
 * support, and explicit field contracts for API consumers.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Represents a single business type option for seller registration")
public class BusinessTypeResponse {

    /**
     * Machine-readable business type code. Used as the value submitted in registration requests.
     * Example: {@code FARMER}, {@code WHOLESALER}, {@code RETAILER}
     */
    @Schema(
            description = "Machine-readable business type code",
            example = "FARMER",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    /** Human-readable display name for UI rendering. Example: {@code "Farmer / Producer"} */
    @Schema(
            description = "Human-readable display label",
            example = "Farmer / Producer",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String displayName;

    /** Optional descriptive text shown in help tooltips or registration guidance. */
    @Schema(
            description = "Optional description for registration guidance",
            example = "A business owned and operated by a single individual",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String description;

    /**
     * Display order for consistent UI rendering across environments. Lower values are displayed
     * first.
     */
    @Schema(
            description = "Display order (ascending, lower = first)",
            example = "1",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer displayOrder;
}
