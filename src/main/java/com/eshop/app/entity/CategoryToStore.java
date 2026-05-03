package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "category_to_store", indexes = {
    @Index(name = "idx_category_store_category", columnList = "category_id"),
    @Index(name = "idx_category_store_store", columnList = "store_id"),
    @Index(name = "idx_category_store_unique", columnList = "category_id,store_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryToStore extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean visible = true;
}
