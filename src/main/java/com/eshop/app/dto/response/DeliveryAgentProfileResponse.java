package com.eshop.app.dto.response;

import com.eshop.app.enums.DeliveryAgentStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeliveryAgentProfileResponse {
    private Long id;
    private Long userId;
    private String vehicleType;
    private String licenseNumber;
    private String zone;
    private DeliveryAgentStatus status;
}
