package com.eshop.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Master entity representing a taluk (also called tehsil) within a district.
 *
 * <p>Unique per district by {@code name} — enforced by DB constraint
 * {@code uq_taluks_district_name}.
 */
@Entity
@Table(
    name = "taluks",
    indexes = @Index(name = "idx_taluks_district_id", columnList = "district_id"),
    uniqueConstraints = @UniqueConstraint(name = "uq_taluks_district_name", columnNames = {"district_id", "name"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "district")
@EqualsAndHashCode(callSuper = true, exclude = "district")
public class Taluk extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String slug;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
