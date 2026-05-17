package com.eshop.app.shipping.application.port.in;

import com.eshop.app.shipping.api.request.DeliveryAgentRegisterRequest;
import com.eshop.app.user.api.response.DeliveryAgentProfileResponse;
import java.util.List;

/**
 * Inbound Port for Delivery Agent Use Cases.
 */
public interface DeliveryAgentUseCase {
    DeliveryAgentProfileResponse registerDeliveryAgent(Long userId, DeliveryAgentRegisterRequest request);
    List<DeliveryAgentProfileResponse> getPendingAgents();
    void approveAgent(Long agentId);
    void rejectAgent(Long agentId);
}
