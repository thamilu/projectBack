package com.eshop.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"message", "version", "description", "status", "timestamp", "documentation", "links"})
@Schema(description = "Welcome page response")
public class WelcomeResponse {
    private String message;
    private String version;
    private String description;
    private String status;
    private Instant timestamp;
    private DocumentationLinks documentation;
    private Map<String, String> links;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentationLinks {
        private String swaggerUi;
        private String openApiJson;
        private String openApiYaml;
        private String postmanCollection;
        private String asyncApi;
    }
}
