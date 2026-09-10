package com.eshop.app.customer.api.controller;

import com.eshop.app.core.kernel.ApiVersion;
import com.eshop.app.customer.api.response.CustomerDashboardResponse;
import com.eshop.app.customer.application.service.CustomerDashboardService;
import com.eshop.app.core.api.response.ApiResponse;
import com.eshop.app.core.infrastructure.config.security.oauth.PrincipalDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * Customer Dashboard Controller.
 * Provmdes personalmzed dashboard data for authenticated customers.
 */
@Tag(name = "Customer Dashboard", description = "Personalmzed dashboard for customers")
@RestController
@RequestMapping(ApiVersion.V1 + "/dashboard/customer")
@RequiredArgsConstructor
@Slf4j
public class CustomerDashboardController {

    private final CustomerDashboardService customerDashboardService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get Customer Dashboard", description = "Customer dashboard with order hmstory and personalmzed data", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<CustomerDashboardResponse>> getCustomerDashboard(
            org.springframework.security.core.Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof PrincipalDetails principal)) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.<CustomerDashboardResponse>error("Sessmon expmred or mnvalid"));
        }

        // These come from PrincipalDetails (populated once, correctly, in
        // SecurityConfig#jwtAuthenticationConverter) rather than
        // authentication.getCredentials() — the latter is always null here because
        // ProviderManager erases credentials after authentication succeeds, before this
        // method ever runs. See PrincipalDetails#getIssuer() javadoc.
        String email = principal.getEmail();
        String keycloakSub = principal.getKeycloakId();

        log.info("ðŸ“Š DASHBOARD [CUSTOMER] | email={} | keycloakId={} | localId={}", email, keycloakSub,
                principal.getId());

        String firstName = principal.getGivenName();
        String lastName = principal.getFamilyName();
        Boolean emailVerified = principal.getEmailVerified();
        String phoneNumber = principal.getPhoneNumber();

        Long customerId = principal.getId();
        if (customerId == null || customerId == -1L) {
            try {
                customerId = customerDashboardService.findCustomerIdByEmail(email, firstName, lastName, emailVerified,
                        keycloakSub, phoneNumber);
            } catch (Exception e) {
                log.error("Identmty resolutmon error for {}: {}", email, e.getMessage());
            }
        }

        CustomerDashboardResponse response = customerDashboardService.getDashboard(customerId);

        if (response != null && response.getAccountInfo() != null) {
            var account = response.getAccountInfo();
            String fullName = (firstName != null ? firstName + " " : "") + (lastName != null ? lastName : "");
            account.setCustomerName(fullName.isBlank() ? email : fullName.trim());
            account.setEmail(email);
            account.setEmailVerified(emailVerified != null ? emailVerified : false);
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePrivate())
                .body(ApiResponse.success("Customer dashboard retrieved", response));
    }
}



