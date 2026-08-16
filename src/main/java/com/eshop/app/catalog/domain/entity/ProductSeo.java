package com.eshop.app.catalog.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.validator.constraints.URL;

/** ProductSeo entity representing SEO metadata for products. */
@Entity
@Table(name = "product_seo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSeo {

    @Id
    @Column(name = "product_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "fk_product_seo_product"))
    private Product product;

    @Size(max = 160, message = "Meta title should not exceed 160 characters for SEO")
    @Column(name = "meta_title", length = 160)
    private String metaTitle;

    @Size(max = 320, message = "Meta description should not exceed 320 characters for SEO")
    @Column(name = "meta_description", length = 320)
    private String metaDescription;

    @Size(max = 500)
    @Column(name = "meta_keywords", length = 500)
    private String metaKeywords;

    @URL(message = "Invalid canonical URL format")
    @Size(max = 1000)
    @Column(name = "canonical_url", length = 1000)
    private String canonicalUrl;
}
