package com.eshop.app.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Request payload to approve or reject a review")
public class ReviewRequest {

    @Schema(description = "Approval status", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Approval status is required")
    private Boolean approved;

    @Schema(description = "Optional remarks from reviewer")
    private String remarks;
}
