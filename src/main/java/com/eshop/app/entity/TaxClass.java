package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tax_classes", indexes = {
    @Index(name = "idx_tax_class_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxClass extends BaseEntity {
    
    @Column(nullable = false, length = 100)
    private String name; // e.g., "Standard", "Reduced", "Zero"
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
