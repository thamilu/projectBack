package com.eshop.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiStatusResponse {
    private String status;
    private String version;
    private String uptime;
    private Long uptimeMillis;
    private String profile;
    private Instant timestamp;
    private HealthDetails health;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthDetails {
        private String database;
        private String cache;
        private String messaging;
    }
}
