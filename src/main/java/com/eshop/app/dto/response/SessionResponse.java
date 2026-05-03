package com.eshop.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Active session information")
public class SessionResponse {
    private String sessionId;
    private String deviceType;
    private String browser;
    private String os;
    private String ipAddress;
    private String location;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;
    private Boolean current;
}
