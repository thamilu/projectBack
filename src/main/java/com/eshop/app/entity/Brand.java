package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "brands", indexes = {
    @Index(name = "idx_brand_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(callSuper = true, of = {"name"})
public class Brand extends BaseEntity {
    
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(unique = true, length = 150)
    private String slug;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column
    @Builder.Default
    private Boolean featured = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    // SEO Metadata
    @Embedded
    private SeoMetadata seoMetadata;
    
    @OneToMany(mappedBy = "brand", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private final Set<Product> products = new HashSet<>();

    @Column
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;
    // Explicit getter for name (required by some services)
    public String getName() {
        return this.name;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setFeatured(Boolean featured) {
        this.featured = featured;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}