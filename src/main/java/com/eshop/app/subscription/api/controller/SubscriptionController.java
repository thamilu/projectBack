package com.eshop.app.subscription.api.controller;

import com.eshop.app.core.kernel.ApiConstants;
import com.eshop.app.core.api.response.ApiResponse;
import com.eshop.app.subscription.api.request.CreateSubscriptionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * [HARDEN] Subscription management API â€” 4-layer normalization.
 *
 * Previously the subscription module had no API layer.
 * Added to complete the 4-layer standard structure:
 * api/ â†’ application/ â†’ domain/ â†’ infrastructure/
 */
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/subscriptions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Subscriptions", description = "Subscription plan and management APIs")
public class SubscriptionController {

    // Inject SubscriptionService when application layer is fully implemented
    // private final SubscriptionService subscriptionService;

    @Operation(summary = "Get all subscription plans")
    @GetMapping("/plans")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<List<Object>>> getSubscriptionPlans() {
        log.info("GET /subscriptions/plans");
        // Placeholder â€” implement with SubscriptionService
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @Operation(summary = "Get current user subscription")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Object>> getMySubscription() {
        log.info("GET /subscriptions/me");
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Subscribe to a plan")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Object>> subscribe(@Valid @RequestBody CreateSubscriptionRequest request) {
        log.info("POST /subscriptions â€” planId={}", request.getPlanId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Cancel subscription")
    @DeleteMapping("/{subscriptionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long subscriptionId) {
        log.info("DELETE /subscriptions/{}", subscriptionId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}


