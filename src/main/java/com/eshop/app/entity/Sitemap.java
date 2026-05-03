package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sitemaps", indexes = {
    @Index(name = "idx_sitemap_url", columnList = "url"),
    @Index(name = "idx_sitemap_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sitemap extends BaseEntity {
    
    @Column(nullable = false, length = 500)
    private String url;
    
    @Column(name = "change_freq", length = 20)
    @Builder.Default
    private String changeFreq = "weekly";
    
    @Column(nullable = false)
    @Builder.Default
    private Double priority = 0.5;
    
    @Column(name = "last_modified")
    private LocalDateTime lastModified;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @Column(name = "entity_type", length = 50)
    private String entityType;
    
    @Column(name = "entity_id")
    private Long entityId;
}
