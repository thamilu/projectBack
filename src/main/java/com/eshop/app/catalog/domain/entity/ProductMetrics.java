package com.eshop.app.catalog.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import lombok.*;

/** ProductMetrics entity representing analytics counters, review metrics, and ratings. */
@Entity
@Table(name = "product_metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductMetrics {

    @Id
    @Column(name = "product_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "fk_product_metrics_product"))
    private Product product;

    @Min(value = 0)
    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;

    @Min(value = 0)
    @Column(name = "purchase_count")
    @Builder.Default
    private Long purchaseCount = 0L;

    @Min(value = 0)
    @Column(name = "wishlist_count")
    @Builder.Default
    private Integer wishlistCount = 0;

    @Column(name = "popularity_score")
    @Builder.Default
    private Double popularityScore = 0.0;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "5.0")
    @Column(name = "average_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Min(value = 0)
    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @Min(value = 0)
    @Column(name = "rating_5_count")
    @Builder.Default
    private Integer rating5Count = 0;

    @Min(value = 0)
    @Column(name = "rating_4_count")
    @Builder.Default
    private Integer rating4Count = 0;

    @Min(value = 0)
    @Column(name = "rating_3_count")
    @Builder.Default
    private Integer rating3Count = 0;

    @Min(value = 0)
    @Column(name = "rating_2_count")
    @Builder.Default
    private Integer rating2Count = 0;

    @Min(value = 0)
    @Column(name = "rating_1_count")
    @Builder.Default
    private Integer rating1Count = 0;
}
