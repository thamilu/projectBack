package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "category_attribute_mappings", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"category_id", "attribute_definition_id"}),
    indexes = {
        @Index(name = "idx_cam_category", columnList = "category_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryAttributeMapping extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_definition_id", nullable = false)
    private AttributeDefinition attributeDefinition;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean required = false;

    @Column(name = "display_order")
    private Integer displayOrder;
}
