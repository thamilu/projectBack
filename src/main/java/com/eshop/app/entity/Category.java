package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_category_name", columnList = "name"),
        @Index(name = "idx_category_parent", columnList = "parent_id"),
        @Index(name = "idx_category_name_parent", columnList = "name, parent_id"),
        @Index(name = "idx_category_slug", columnList = "slug"),
        @Index(name = "idx_category_path", columnList = "path"),
        @Index(name = "idx_category_depth", columnList = "depth"),
        @Index(name = "idx_category_active", columnList = "active")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_category_name_parent", columnNames = { "name", "parent_id" }),
        @UniqueConstraint(name = "uk_category_slug", columnNames = { "slug" })
})
@Getter
@Setter
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(callSuper = true, of = {"name"})
public class Category extends BaseEntity {
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(unique = true, length = 150)
    private String slug;
    
    // Materialized path for efficient tree queries
    // Example: "Electronics > Mobiles > Smartphones"
    @Column(name = "path", length = 1000)
    private String path;

    // Hierarchy depth (0 = root level)
    @Column(name = "depth")
    @Builder.Default
    private Integer depth = 0;

    // Display order within siblings
    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    // SEO Metadata
    @Embedded
    private SeoMetadata seoMetadata;
    

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @ToString.Exclude
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @Builder.Default
    @ToString.Exclude
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Set<Category> subCategories = new HashSet<>();

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Set<Product> products = new HashSet<>();

    // Convenience constructor used in some service code
    public Category(String name) {
        this.name = name;
    }

    // Explicitly initialize final fields in the no-args constructor
    public Category() {
        this.active = true;
        this.depth = 0;
        this.displayOrder = 0;
        this.products = new HashSet<>();
        this.subCategories = new HashSet<>();
    }

    // Utility method to get full hierarchical path
    public String getFullPath() {
        if (parent == null) {
            return name;
        }
        return parent.getFullPath() + " > " + name;
    }

    // Utility method to add a child category
    public void addChild(Category child) {
        this.subCategories.add(child);
        child.setParent(this);
        if (this.depth != null) {
            child.setDepth(this.depth + 1);
        }
    }

    // Explicit getters and setters for compatibility
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return this.description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
}