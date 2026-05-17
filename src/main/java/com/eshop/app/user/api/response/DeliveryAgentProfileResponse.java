package com.eshop.app.user.api.response;





import lombok.Builder;
import lombok.Data;
import com.eshop.app.user.shared.domain.enums.DeliveryAgentStatus;

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

