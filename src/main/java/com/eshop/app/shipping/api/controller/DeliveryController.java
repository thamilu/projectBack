package com.eshop.app.shipping.api.controller;

import com.eshop.app.core.util.SecurityUtils;
import static com.eshop.app.core.infrastructure.config.security.SecurityExpressions.IS_ADMIN_OR_CUSTOMER_OR_DELIVERY_AGENT;


import com.eshop.app.shipping.api.request.DeliveryAgentRegisterRequest;
import com.eshop.app.shipping.application.port.in.DeliveryAgentUseCase;
import com.eshop.app.user.api.response.DeliveryAgentProfileResponse;




import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/delivery")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Delivery Agents", description = "Delivery agent registration and profile management")
@SecurityRequirement(name = "bearerAuth")
public class DeliveryController {

    private final DeliveryAgentUseCase deliveryAgentService;

    @PostMapping("/register")
    @org.springframework.security.access.prepost.PreAuthorize(IS_ADMIN_OR_CUSTOMER_OR_DELIVERY_AGENT)
    @Operation(summary = "Register as a Delivery Agent")
    public ResponseEntity<DeliveryAgentProfileResponse> register(
            @Valid @RequestBody DeliveryAgentRegisterRequest request,
            Authentication authentication) {

        Long userId = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.ok(deliveryAgentService.registerDeliveryAgent(userId, request));
    }
}




