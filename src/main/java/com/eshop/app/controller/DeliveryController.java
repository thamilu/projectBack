package com.eshop.app.controller;

import com.eshop.app.dto.request.DeliveryAgentRegisterRequest;
import com.eshop.app.dto.response.DeliveryAgentProfileResponse;
import com.eshop.app.service.DeliveryAgentService;
import com.eshop.app.service.SellerService; // Reusing user resolution logic or I should extract it
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/delivery")
@RequiredArgsConstructor
@Tag(name = "Delivery Agents", description = "Delivery agent registration and profile management")
@SecurityRequirement(name = "bearerAuth")
public class DeliveryController {

    private final DeliveryAgentService deliveryAgentService;
    private final SellerService sellerService; // Using this for resolveUserId for now to avoid duplication

    @PostMapping("/register")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('CUSTOMER', 'DELIVERY_AGENT', 'ADMIN')")
    @Operation(summary = "Register as a Delivery Agent")
    public ResponseEntity<DeliveryAgentProfileResponse> register(
            @Valid @RequestBody DeliveryAgentRegisterRequest request,
            Authentication authentication) {

        Long userId = sellerService.resolveUserId(authentication);
        return ResponseEntity.ok(deliveryAgentService.registerDeliveryAgent(userId, request));
    }
}
