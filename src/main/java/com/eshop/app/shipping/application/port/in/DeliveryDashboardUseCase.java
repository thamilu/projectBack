package com.eshop.app.shipping.application.port.in;

import com.eshop.app.shipping.api.response.DeliveryDashboardResponse;

/**
 * Inbound Port for Delivery Dashboard Use Cases.
 */
public interface DeliveryDashboardUseCase {
    DeliveryDashboardResponse getDashboard(Long agentId);
}
