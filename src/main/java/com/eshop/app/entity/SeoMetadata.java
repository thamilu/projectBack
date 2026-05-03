package com.eshop.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeoMetadata {
    
    @Column(name = "meta_title", length = 200)
    private String metaTitle;
    
    @Column(name = "meta_description", length = 500)
    private String metaDescription;
    
    @Column(name = "meta_keywords", length = 500)
    private String metaKeywords;
    
    @Column(name = "og_title", length = 200)
    private String ogTitle;
    
    @Column(name = "og_description", length = 500)
    private String ogDescription;
    
    @Column(name = "og_image", length = 500)
    private String ogImage;
    
    @Column(name = "twitter_card", length = 20)
    @Builder.Default
    private String twitterCard = "summary_large_image";
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema_data", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> schemaData = new HashMap<>();
}
