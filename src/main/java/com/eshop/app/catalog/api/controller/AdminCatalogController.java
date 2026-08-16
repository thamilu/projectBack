package com.eshop.app.catalog.api.controller;

import com.eshop.app.catalog.api.response.ProductDuplicateCandidateResponse;
import com.eshop.app.catalog.application.port.in.CatalogMergeUseCase;
import com.eshop.app.core.api.response.ApiResponse;
import com.eshop.app.core.api.response.PageResponse;
import com.eshop.app.core.kernel.ApiConstants;
import static com.eshop.app.core.infrastructure.config.security.SecurityExpressions.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Controller exposing administrative endpoints to list, merge, or dismiss duplicate master catalog
 * similarity matching warnings.
 */
@io.swagger.v3.oas.annotations.tags.Tag(
        name = "Admin Catalog Management",
        description = "Administrative catalog review, deduplication, and merging tools")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/admin/catalog")
@PreAuthorize(IS_ADMIN)
@Validated
@Slf4j
@RequiredArgsConstructor
public class AdminCatalogController {

    private final CatalogMergeUseCase mergeService;

    @GetMapping("/duplicate-candidates")
    @Operation(
            summary = "Get duplicate product candidates",
            description = "List all flagged similar duplicate master products pending review.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<PageResponse<ProductDuplicateCandidateResponse>>>
            getDuplicateCandidates(
                    @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        log.info("Admin request: Fetch duplicate master candidates pending review");
        PageResponse<ProductDuplicateCandidateResponse> candidates =
                mergeService.getDuplicateCandidates(pageable);
        return ResponseEntity.ok(
                ApiResponse.success("Duplicate candidates retrieved successfully", candidates));
    }

    @PostMapping("/merge")
    @Operation(
            summary = "Merge duplicate products",
            description =
                    "Merges source catalog product into target catalog product, relinking all"
                            + " seller storefront listings.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Void>> mergeMasterProducts(
            @RequestParam Long sourceId, @RequestParam Long targetId) {
        log.info(
                "Admin request: Merge duplicate product ID {} into target ID {}",
                sourceId,
                targetId);
        mergeService.mergeMasterProducts(sourceId, targetId);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Catalog products merged successfully. All listings updated.", null));
    }

    @PostMapping("/dismiss-candidate/{id}")
    @Operation(
            summary = "Dismiss duplicate candidate warning",
            description =
                    "Dismiss a similarity warning, keeping catalog items separate and flagging them"
                            + " as verified distinct.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Void>> dismissCandidate(@PathVariable Long id) {
        log.info("Admin request: Dismiss duplicate check candidate warning ID {}", id);
        mergeService.dismissCandidate(id);
        return ResponseEntity.ok(
                ApiResponse.success("Similarity warning candidate dismissed.", null));
    }
}
