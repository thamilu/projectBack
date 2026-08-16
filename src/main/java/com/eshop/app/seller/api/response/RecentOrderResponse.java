package com.eshop.app.seller.api.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO representing a recent order entry for the seller dashboard analytics summary.
 * Designed for light aggregate views rather than detailed order lists.
 *
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Seller recent order details summary")
public class RecentOrderResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Unique customer-visible alphanumeric order identifier. */
    @Schema(description = "Unique customer-visible alphanumeric order number. Immutable and globally unique.",
            example = "ORD-2026-0704-9872")
    private String orderNumber;

    /** Current lifecycle status of the order. */
    @Schema(description = "Current lifecycle status of the order. Possible values: PLACED, PROCESSING, SHIPPED, DELIVERED, CANCELLED.",
            example = "PLACED")
    private String status;

    /** Total transaction amount for the order. */
    @MonetaryField
    @Schema(description = "Total transaction amount for the order, formatted to 2 decimal places using MoneySerializer.",
            example = "149.99")
    private BigDecimal totalAmount;

    /** Timestamp when the order checkout was completed. */
    @Schema(description = "Timestamp when the order was completed at checkout, in local server time format (yyyy-MM-dd'T'HH:mm:ss).",
            example = "2026-07-03T14:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
