package com.eshop.app.location.domain.entity;

import com.eshop.app.core.kernel.BaseEntity;




import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Master entity representing a district within a state.
 *
 * <p>Unique per state by {@code name} — enforced by DB constraint
 * {@code uq_districts_state_name}.
 */
@Entity
@Table(
    name = "districts",
    indexes = @Index(name = "idx_districts_state_id", columnList = "state_id"),
    uniqueConstraints = @UniqueConstraint(name = "uq_districts_state_name", columnNames = {"state_id", "name"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "state")
@EqualsAndHashCode(callSuper = true, exclude = "state")
public class District extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "state_id", nullable = false)
    private State state;

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

