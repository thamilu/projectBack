package com.eshop.app.shipping.api.controller;

import com.eshop.app.core.kernel.ApiVersion;
import com.eshop.app.core.api.response.ApiResponse;
import com.eshop.app.core.infrastructure.config.security.oauth.PrincipalDetails;
import com.eshop.app.shipping.api.response.DeliveryDashboardResponse;
import com.eshop.app.shipping.application.port.in.DeliveryDashboardUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * Delivery Agent Dashboard Controller.
 * Provmdes delivery assmgnments and performance metrics for agents.
 */
@Tag(name = "Delivery Agent Dashboard", description = "Dashboard for delivery agents")
@RestController
@RequestMapping(ApiVersion.V1 + "/dashboard/delivery-agent")
@RequiredArgsConstructor
@Slf4j
public class DeliveryDashboardController {

    private final DeliveryDashboardUseCase deliveryDashboardService;

    @GetMapping
    @PreAuthorize("hasRole(@appProperties.security.roles.delivery)")
    @Operation(summary = "Get Delivery Agent Dashboard", description = "Delivery agent dashboard with assigned orders and performance metrics", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<DeliveryDashboardResponse>> getDeliveryAgentDashboard(
            @AuthenticationPrincipal PrincipalDetails principal) {

        Long agentId = principal.getId();
        String email = principal.getEmail();

        log.info("Delivery dashboard requested for agent: {}", email);

        DeliveryDashboardResponse response = deliveryDashboardService.getDashboard(agentId);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(2, TimeUnit.MINUTES).cachePrivate())
                .body(ApiResponse.success("Delivery dashboard retrieved", response));
    }
}



