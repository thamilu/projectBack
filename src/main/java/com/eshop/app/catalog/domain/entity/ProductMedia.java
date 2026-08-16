package com.eshop.app.catalog.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

/** Product media entity representing shared image or video assets in the master catalog. */
@Entity
@Table(
        name = "product_media",
        indexes = {@Index(name = "idx_product_media_master_id", columnList = "master_product_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProductMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_product_id", nullable = false)
    @NotNull(message = "Master product is required")
    private MasterProduct masterProduct;

    @NotBlank(message = "Media URL is required")
    @Size(max = 1000, message = "Media URL cannot exceed 1000 characters")
    @Column(name = "media_url", nullable = false, length = 1000)
    private String mediaUrl;

    @NotBlank(message = "Media type is required")
    @Size(max = 30)
    @Column(name = "media_type", nullable = false, length = 30)
    @Builder.Default
    private String mediaType = "IMAGE";

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Size(max = 255)
    @Column(name = "alt_text", length = 255)
    private String altText;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
