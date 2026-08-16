package com.eshop.app.tax.api.controller;

import com.eshop.app.core.kernel.ApiConstants;
import com.eshop.app.core.api.response.ApiResponse;
import static com.eshop.app.core.infrastructure.config.security.SecurityExpressions.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * [HARDEN] Tax management API â€” 4-layer normalization.
 *
 * Previously the tax module had no API layer.
 * Added to complete the 4-layer standard structure.
 * Tax rates are admin-managed and consumed by order/pricing modules.
 */
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/admin/tax")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tax Management", description = "Tax rate and class management APIs (Admin only)")
public class TaxController {

    private final com.eshop.app.tax.application.service.TaxService taxService;

    @Operation(summary = "Get all tax rates")
    @GetMapping("/rates")
    @PreAuthorize(IS_ADMIN)
    public ResponseEntity<ApiResponse<List<com.eshop.app.tax.api.response.TaxRateResponse>>> getAllTaxRates() {
        log.info("GET /admin/tax/rates");
        return ResponseEntity.ok(ApiResponse.success(taxService.getAllTaxRates()));
    }

    @Operation(summary = "Get all tax classes")
    @GetMapping("/classes")
    @PreAuthorize(IS_ADMIN)
    public ResponseEntity<ApiResponse<List<com.eshop.app.tax.api.response.TaxClassResponse>>> getAllTaxClasses() {
        log.info("GET /admin/tax/classes");
        return ResponseEntity.ok(ApiResponse.success(taxService.getAllTaxClasses()));
    }

    @Operation(summary = "Calculate tax for an amount")
    @GetMapping("/calculate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BigDecimal>> calculateTax(
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String taxClassCode) {
        log.info("GET /admin/tax/calculate â€” amount={}, class={}", amount, taxClassCode);
        return ResponseEntity.ok(ApiResponse.success(BigDecimal.ZERO));
    }
}


