package com.eshop.app.controller;

import com.eshop.app.dto.request.ReviewRequest;
import com.eshop.app.dto.response.CategoryResponse;
import com.eshop.app.service.CategoryRequestService;
import com.eshop.app.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.eshop.app.security.PrincipalDetails;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.eshop.app.constants.ApiConstants;
import com.eshop.app.dto.request.CategoryRequest;

@Tag(name = "Admin Categories", description = "Admin category management - Create and review category requests")
@RestController
@RequestMapping(ApiConstants.Endpoints.ADMIN_CATEGORY)
@RequiredArgsConstructor
public class AdminCategoryController {
    private final CategoryService categoryService;
    private final CategoryRequestService requestService;

    @PostMapping
    @Operation(
        summary = "Create category",
        description = "Create a new category (admin only)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<?> createCategory(@RequestBody CategoryRequest dto) {
        CategoryResponse category = categoryService.createCategory(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    @GetMapping("/requests/pending")
    public ResponseEntity<?> getPendingRequests() {
        return ResponseEntity.ok(requestService.getPendingRequests());
    }

    @PutMapping("/requests/{requestId}/review")
    public ResponseEntity<?> reviewRequest(
            @PathVariable Long requestId,
            @RequestBody ReviewRequest dto,
            @AuthenticationPrincipal PrincipalDetails principal) {
        Long adminId = principal.getId();
        var request = requestService.reviewRequest(requestId, dto, adminId);
        return ResponseEntity.ok(new com.eshop.app.dto.response.CategoryRequestResponse(request));
    }
}
