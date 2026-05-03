package com.eshop.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Two-factor setup response")
public class TwoFactorSetupResponse {
    private String secret;
    private String qrCodeImage;
    private List<String> recoveryCodes;
}
