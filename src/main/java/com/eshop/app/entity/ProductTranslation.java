package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_translations", indexes = {
    @Index(name = "idx_product_translation_product", columnList = "product_id"),
    @Index(name = "idx_product_translation_language", columnList = "language_id"),
    @Index(name = "idx_product_translation_unique", columnList = "product_id,language_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductTranslation extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;
    
    @Column(nullable = false, length = 200)
    private String name;
    
    @Column(length = 2000)
    private String description;
    
    @Column(name = "meta_title", length = 200)
    private String metaTitle;
    
    @Column(name = "meta_description", length = 500)
    private String metaDescription;
    
    @Column(name = "meta_keywords", length = 500)
    private String metaKeywords;
    
    @Column(name = "friendly_url", length = 200)
    private String friendlyUrl;
}
