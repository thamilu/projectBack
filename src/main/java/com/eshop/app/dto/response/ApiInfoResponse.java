package com.eshop.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "API information response")
public class ApiInfoResponse {
    @Schema(description = "API name", example = "EShop API")
    private String name;
    @Schema(description = "API version", example = "1.0.0")
    private String version;
    @Schema(description = "API description")
    private String description;
    @Schema(description = "Available features")
    private Map<String, String> features;
    @Schema(description = "Available endpoints")
    private Map<String, String> endpoints;
    @Schema(description = "Technology stack")
    private List<String> technologies;
    @Schema(description = "Response timestamp")
    private Instant timestamp;
    @Schema(description = "API status", example = "operational")
    private String status;
}
