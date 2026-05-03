package com.eshop.app.controller;

import com.eshop.app.dto.request.CategoryRequest;
import com.eshop.app.service.CategoryRequestService;
import com.eshop.app.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.eshop.app.security.PrincipalDetails;
import org.springframework.web.bind.annotation.*;
import com.eshop.app.constants.ApiConstants;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.eshop.app.dto.response.CategoryResponse;

@Tag(name = "Seller Categories", description = "Seller category requests - Submit and track category creation requests")
@RestController
@RequestMapping(ApiConstants.Endpoints.SELLER_CATEGORY)
@RequiredArgsConstructor
public class SellerCategoryController {
    private final CategoryService categoryService;
    private final CategoryRequestService requestService;

    @GetMapping
    @Operation(summary = "List categories", description = "Get all categories available to sellers")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @PostMapping("/request")
    @Operation(
        summary = "Request new category",
        description = "Seller suggests/request a new category. Admins will review and approve or reject.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<?> requestNewCategory(
            @RequestBody CategoryRequest dto,
            @AuthenticationPrincipal PrincipalDetails principal) {
        Long sellerId = principal.getId();
        var request = requestService.createRequest(dto, sellerId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new com.eshop.app.dto.response.CategoryRequestResponse(request));
    }

    @GetMapping("/my-requests")
    @Operation(summary = "Get my category requests", description = "Get category requests submitted by current seller", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<?> getMyRequests(
            @AuthenticationPrincipal PrincipalDetails principal) {
        Long sellerId = principal.getId();
        return ResponseEntity.ok(requestService.getSellerRequests(sellerId));
    }
}
