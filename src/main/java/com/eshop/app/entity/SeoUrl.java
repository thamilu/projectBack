package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seo_urls", indexes = {
    @Index(name = "idx_seo_url_keyword", columnList = "keyword", unique = true),
    @Index(name = "idx_seo_url_entity", columnList = "entity_type,entity_id"),
    @Index(name = "idx_seo_url_language", columnList = "language_id"),
    @Index(name = "idx_seo_url_store", columnList = "store_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeoUrl extends BaseEntity {
    
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // PRODUCT, CATEGORY, BRAND, PAGE
    
    @Column(name = "entity_id", nullable = false)
    private Long entityId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id")
    private Language language;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;
    
    @Column(nullable = false, unique = true, length = 255)
    private String keyword; // URL slug
    
    @Column(name = "is_canonical", nullable = false)
    @Builder.Default
    private Boolean isCanonical = true;
}
