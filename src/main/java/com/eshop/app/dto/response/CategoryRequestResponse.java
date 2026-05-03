package com.eshop.app.dto.response;

import com.eshop.app.entity.CategoryRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Response for a category request")
public class CategoryRequestResponse {
    private Long id;
    private String categoryName;
    private String description;
    private String reason;
    private String status;
    private String adminRemarks;
    private String sellerName;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    public CategoryRequestResponse(CategoryRequest request) {
        this.id = request.getId();
        this.categoryName = request.getCategoryName();
        this.description = request.getDescription();
        this.reason = request.getReason();
        this.status = request.getStatus().name();
        this.adminRemarks = request.getAdminRemarks();
        this.sellerName = request.getSeller().getUsername();
        this.createdAt = request.getCreatedAt();
        this.reviewedAt = request.getReviewedAt();
    }
}
