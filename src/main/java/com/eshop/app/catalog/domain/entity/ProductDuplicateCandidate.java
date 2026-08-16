package com.eshop.app.catalog.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

/** Entity representing flagged duplicate candidates within the master catalog. */
@Entity
@Table(name = "product_duplicate_candidates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProductDuplicateCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "source_product_id", nullable = false)
    private Long sourceProductId;

    @Column(name = "matched_product_id", nullable = false)
    private Long matchedProductId;

    @Column(name = "similarity_score", nullable = false)
    private Double similarityScore;

    @Column(name = "review_status", nullable = false, length = 50)
    @Builder.Default
    private String reviewStatus = "PENDING"; // PENDING, MERGED, DISMISSED

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
