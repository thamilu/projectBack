package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "languages", indexes = {
    @Index(name = "idx_language_code", columnList = "code", unique = true),
    @Index(name = "idx_language_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Language extends BaseEntity {
    
    @Column(nullable = false, unique = true, length = 10)
    private String code; // ISO 639-1 code (e.g., "en", "es", "ar")
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false, length = 10)
    private String locale; // e.g., "en_US", "es_ES"
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean rtl = false; // Right-to-Left support
    
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
    
    @Column(name = "flag_icon", length = 255)
    private String flagIcon;
}
