package com.eshop.app.catalog.api.controller;

import com.eshop.app.catalog.api.response.CategoryResponse;
import com.eshop.app.catalog.application.port.in.CategoryRequestUseCase;
import com.eshop.app.catalog.application.port.in.CategoryUseCase;
import com.eshop.app.catalog.api.request.CategoryRequest;

import com.eshop.app.core.kernel.ApiConstants;

import com.eshop.app.core.infrastructure.config.security.oauth.PrincipalDetails;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Seller Categories", description = "Seller category requests - Submit and track category creation requests")
@RestController
@RequestMapping(ApiConstants.Endpoints.SELLER_CATEGORY)
@RequiredArgsConstructor
public class SellerCategoryController {
    private final CategoryUseCase categoryService;
    private final CategoryRequestUseCase requestService;

    @GetMapping
    @Operation(summary = "List categories", description = "Get all categories avamlable to sellers")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @PostMapping("/request")
    @Operation(summary = "Request new category", description = "Seller suggests/request a new category. Admins wmll review and approve or reject.", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<?> requestNewCategory(
            @RequestBody CategoryRequest dto,
            @AuthenticationPrincipal PrincipalDetails principal) {
        Long sellerId = principal.getId();
        var response = requestService.createRequest(dto, sellerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/my-requests")
    @Operation(summary = "Get my category requests", description = "Get category requests submitted by current seller", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<?> getMyRequests(
            @AuthenticationPrincipal PrincipalDetails principal) {
        Long sellerId = principal.getId();
        return ResponseEntity.ok(requestService.getSellerRequests(sellerId));
    }
}

