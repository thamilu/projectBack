package com.eshop.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeliveryAgentRegisterRequest {

    @NotBlank(message = "Vehicle type is required")
    private String vehicleType;

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    @NotBlank(message = "Zone is required")
    private String zone;
}
