package com.eshop.app.catalog.api.controller;

import com.eshop.app.catalog.api.request.ReviewRequest;
import com.eshop.app.catalog.api.response.CategoryResponse;
import com.eshop.app.catalog.application.port.in.CategoryUseCase;
import com.eshop.app.catalog.application.port.in.CategoryRequestUseCase;
import com.eshop.app.catalog.api.request.CategoryRequest;
import com.eshop.app.core.kernel.ApiConstants;
import com.eshop.app.core.infrastructure.config.security.oauth.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Admin Categories", description = "Admin category management - Create and review category requests")
@RestController
@RequestMapping(ApiConstants.Endpoints.ADMIN_CATEGORY)
@RequiredArgsConstructor
public class AdminCategoryController {
    private final CategoryUseCase categoryService;
    private final CategoryRequestUseCase requestService;

    @PostMapping
    @Operation(summary = "Create category", description = "Create a new category (admin only)", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<?> createCategory(@RequestBody CategoryRequest dto) {
        CategoryResponse category = categoryService.createCategory(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    @GetMapping({"/requests/pending", "/requests/pendmng"})
    public ResponseEntity<?> getPendingRequests() {
        return ResponseEntity.ok(requestService.getPendingRequests());
    }

    @PutMapping("/requests/{requestId}/review")
    public ResponseEntity<?> reviewRequest(
            @PathVariable Long requestId,
            @RequestBody ReviewRequest dto,
            @AuthenticationPrincipal PrincipalDetails principal) {
        Long adminId = principal.getId();
        var response = requestService.reviewRequest(requestId, dto, adminId);
        return ResponseEntity.ok(response);
    }
}
