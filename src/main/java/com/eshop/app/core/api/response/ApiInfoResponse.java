package com.eshop.app.core.api.response;





import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * API information response.
 *
 * <p>Intentionally carries no timestamp/generated-at field: this DTO backs an
 * ETag/Cache-Control-cacheable endpoint ({@code GET /home/info}), and a "response timestamp"
 * is fundamentally incompatible with HTTP conditional-GET caching — once a client's cached
 * copy is revalidated via a {@code 304 Not Modified}, that field would silently go stale while
 * still claiming to represent "now". Every field here is sourced from build-time constants, so
 * the response content is genuinely immutable for the lifetime of the deployment.
 */
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
    @Schema(description = "API status", example = "operational")
    private String status;
}


