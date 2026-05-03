package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "category_translations", indexes = {
    @Index(name = "idx_category_translation_category", columnList = "category_id"),
    @Index(name = "idx_category_translation_language", columnList = "language_id"),
    @Index(name = "idx_category_translation_unique", columnList = "category_id,language_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryTranslation extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 1000)
    private String description;
    
    @Column(name = "meta_title", length = 200)
    private String metaTitle;
    
    @Column(name = "meta_description", length = 500)
    private String metaDescription;
    
    @Column(name = "meta_keywords", length = 500)
    private String metaKeywords;
}
