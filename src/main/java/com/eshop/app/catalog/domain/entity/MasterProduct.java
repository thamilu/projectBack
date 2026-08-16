package com.eshop.app.catalog.domain.entity;

import com.eshop.app.core.kernel.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.BatchSize;

/** Master catalog product representing a global/shared marketplace item definition. */
@Entity
@Table(
        name = "master_products",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_master_product_slug", columnNames = "slug")
        },
        indexes = {
            @Index(name = "idx_master_product_category", columnList = "category_id"),
            @Index(name = "idx_master_product_brand", columnList = "brand_id"),
            @Index(name = "idx_master_product_status", columnList = "approval_status, is_active"),
            @Index(name = "idx_master_product_parent_id", columnList = "parent_master_product_id"),
            @Index(name = "idx_master_product_root_id", columnList = "root_master_product_id"),
            @Index(name = "idx_master_product_type", columnList = "product_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
@BatchSize(size = 25)
public class MasterProduct extends BaseEntity {

    @Size(max = 255, message = "Name must be under 255 characters")
    @Column(name = "name", nullable = true, length = 255)
    @ToString.Include
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(max = 255)
    @Column(name = "slug", nullable = false, unique = true, length = 255)
    @ToString.Include
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", foreignKey = @ForeignKey(name = "fk_master_product_brand"))
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "category_id",
            foreignKey = @ForeignKey(name = "fk_master_product_category"),
            nullable = true)
    private Category category;

    @Column(name = "base_description", columnDefinition = "TEXT")
    private String baseDescription;

    @Size(max = 500, message = "Short description cannot exceed 500 characters")
    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "specifications", columnDefinition = "TEXT")
    private String specifications;

    @NotNull(message = "Approval status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 30)
    @Builder.Default
    private MasterProductApprovalStatus approvalStatus = MasterProductApprovalStatus.APPROVED;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "duplicate_status", length = 50)
    @Builder.Default
    private String duplicateStatus = "NONE";

    @Column(name = "merged_into_product_id")
    private Long mergedIntoProductId;

    @Column(name = "created_by_seller_id", length = 100)
    private String createdBySellerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "parent_master_product_id",
            foreignKey = @ForeignKey(name = "fk_master_product_parent"))
    private MasterProduct parentMasterProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "root_master_product_id",
            foreignKey = @ForeignKey(name = "fk_master_product_root"))
    private MasterProduct rootMasterProduct;

    @NotNull(message = "Product type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 30)
    @Builder.Default
    private MasterProductType productType = MasterProductType.MASTER;

    @Column(name = "derived_from_seller_id", length = 100)
    private String derivedFromSellerId;

    @Column(name = "derived_reason", length = 500)
    private String derivedReason;

    @Column(name = "created_from_catalog", nullable = false)
    @Builder.Default
    private boolean createdFromCatalog = false;

    @OneToMany(mappedBy = "masterProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    @BatchSize(size = 20)
    @Builder.Default
    private List<ProductMedia> media = new ArrayList<>();

    @OneToMany(mappedBy = "masterProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    @BatchSize(size = 20)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    // ==================== CUSTOM GETTERS FOR FALLBACK ENGINE ====================

    public String getName() {
        if (this.name != null && !this.name.trim().isEmpty()) {
            return this.name;
        }
        return this.parentMasterProduct != null ? this.parentMasterProduct.getName() : this.name;
    }

    public Brand getBrand() {
        if (this.brand != null) {
            return this.brand;
        }
        return this.parentMasterProduct != null ? this.parentMasterProduct.getBrand() : this.brand;
    }

    public Category getCategory() {
        if (this.category != null) {
            return this.category;
        }
        return this.parentMasterProduct != null
                ? this.parentMasterProduct.getCategory()
                : this.category;
    }

    public String getBaseDescription() {
        if (this.baseDescription != null && !this.baseDescription.trim().isEmpty()) {
            return this.baseDescription;
        }
        return this.parentMasterProduct != null
                ? this.parentMasterProduct.getBaseDescription()
                : this.baseDescription;
    }

    public String getShortDescription() {
        if (this.shortDescription != null && !this.shortDescription.trim().isEmpty()) {
            return this.shortDescription;
        }
        return this.parentMasterProduct != null
                ? this.parentMasterProduct.getShortDescription()
                : this.shortDescription;
    }

    public String getSpecifications() {
        if (this.specifications != null && !this.specifications.trim().isEmpty()) {
            return this.specifications;
        }
        return this.parentMasterProduct != null
                ? this.parentMasterProduct.getSpecifications()
                : this.specifications;
    }

    public List<ProductMedia> getMedia() {
        if (this.media != null && !this.media.isEmpty()) {
            return this.media;
        }
        return this.parentMasterProduct != null ? this.parentMasterProduct.getMedia() : this.media;
    }

    // ==================== SEMANTIC LOGIC METHODS ====================

    /** Add a media asset to this master product. */
    public void addMedia(ProductMedia productMedia) {
        media.add(productMedia);
        productMedia.setMasterProduct(this);
    }

    /** Remove a media asset. */
    public void removeMedia(ProductMedia productMedia) {
        media.remove(productMedia);
        productMedia.setMasterProduct(null);
    }

    /** Add a product image to this master product. */
    public void addImage(ProductImage image) {
        images.add(image);
        image.setMasterProduct(this);
    }

    /** Remove a product image from this master product. */
    public void removeImage(ProductImage image) {
        images.remove(image);
        image.setMasterProduct(null);
    }
}
