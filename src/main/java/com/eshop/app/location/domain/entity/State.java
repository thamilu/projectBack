package com.eshop.app.location.domain.entity;

import com.eshop.app.core.kernel.BaseEntity;




import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Master entity representing a state or union territory.
 *
 * <p>Unique per country by {@code stateCode} — enforced by DB constraint
 * {@code uq_states_country_code}.
 */
@Entity
@Table(
    name = "states",
    indexes = @Index(name = "idx_states_country_id", columnList = "country_id"),
    uniqueConstraints = @UniqueConstraint(name = "uq_states_country_code", columnNames = {"country_id", "state_code"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "country")
@EqualsAndHashCode(callSuper = true, exclude = "country")
public class State extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * ISO-style state code, e.g. "KA" (Karnataka), "MH" (Maharashtra).
     * Useful for tax systems and analytics integrations.
     */
    @NotBlank
    @Size(max = 10)
    @Column(name = "state_code", nullable = false, length = 10)
    private String stateCode;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String slug;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}

