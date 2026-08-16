package com.eshop.app.user.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standardized response for user logout actions")
public class LogoutResponse {

    @Schema(
            description = "Human-readable status or confirmation message",
            example = "Logged out successfully")
    private String message;

    @Schema(description = "Timestamp when the logout action was completed")
    private Instant timestamp;

    @Schema(
            description = "ID of the terminated session, if available",
            example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
    private String sessionId;
}
