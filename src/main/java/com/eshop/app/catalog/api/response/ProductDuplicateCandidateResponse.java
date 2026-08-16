package com.eshop.app.catalog.api.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDuplicateCandidateResponse {
    private Long id;
    private Long sourceProductId;
    private Long matchedProductId;
    private Double similarityScore;
    private String reviewStatus;
    private LocalDateTime createdAt;
}
