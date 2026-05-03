package com.eshop.app.entity;

import com.eshop.app.entity.enums.AttributeType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "attribute_definitions", indexes = {
    @Index(name = "idx_attr_def_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeDefinition extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name; // Internal key, e.g., 'processor_speed'

    @Column(nullable = false, length = 100)
    private String label; // Display label, e.g., 'Processor Speed'

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    private AttributeType dataType;

    @Column(columnDefinition = "TEXT")
    private String options; // JSON array for SELECT options: ["i5", "i7"]

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
