package com.eshop.app.controller;

import com.eshop.app.dto.response.DeliveryAgentProfileResponse;
import com.eshop.app.dto.response.SellerProfileResponse;
import com.eshop.app.service.DeliveryAgentService;
import com.eshop.app.service.SellerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/approvals")
@RequiredArgsConstructor
@Tag(name = "Admin Approvals", description = "Admin endpoints for approving sellers and delivery agents")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminApprovalController {

    private final SellerService sellerService;
    private final DeliveryAgentService deliveryAgentService;

    // --- Sellers ---

    @GetMapping("/sellers")
    @Operation(summary = "List all pending seller applications")
    public ResponseEntity<List<SellerProfileResponse>> getPendingSellers() {
        return ResponseEntity.ok(sellerService.getPendingSellers());
    }

    @PostMapping("/sellers/{id}/{action}")
    @Operation(summary = "Approve or Reject a seller application")
    public ResponseEntity<Void> handleSellerAction(
            @PathVariable Long id,
            @PathVariable String action,
            Authentication authentication) {

        String processedBy = (authentication != null) ? authentication.getName() : "system";

        if ("APPROVE".equalsIgnoreCase(action)) {
            sellerService.approveSeller(id, processedBy);
        } else if ("REJECT".equalsIgnoreCase(action)) {
            sellerService.rejectSeller(id, "Rejected by administrative action", processedBy);
        } else {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();
    }

    // --- Delivery Agents ---

    @GetMapping("/delivery-agents")
    @Operation(summary = "List all pending delivery agent applications")
    public ResponseEntity<List<DeliveryAgentProfileResponse>> getPendingDeliveryAgents() {
        return ResponseEntity.ok(deliveryAgentService.getPendingAgents());
    }

    @PostMapping("/delivery-agents/{id}/{action}")
    @Operation(summary = "Approve or Reject a delivery agent application")
    public ResponseEntity<Void> handleDeliveryAgentAction(
            @PathVariable Long id,
            @PathVariable String action) {

        if ("APPROVE".equalsIgnoreCase(action)) {
            deliveryAgentService.approveAgent(id);
        } else if ("REJECT".equalsIgnoreCase(action)) {
            deliveryAgentService.rejectAgent(id);
        } else {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();
    }
}
