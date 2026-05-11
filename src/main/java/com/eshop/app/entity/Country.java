package com.eshop.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Master entity representing a country.
 * Root of the location hierarchy: Country → State → District → Taluk → PostalCode.
 *
 * <p>Constraints:
 * <ul>
 *   <li>{@code isoCode} is globally unique (ISO 3166-1 alpha-2, e.g. "IN").</li>
 * </ul>
 */
@Entity
@Table(
    name = "countries",
    uniqueConstraints = @UniqueConstraint(name = "uq_countries_iso_code", columnNames = "iso_code")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(callSuper = true)
public class Country extends BaseEntity {

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    /** ISO 3166-1 alpha-2 code, e.g. "IN", "US". */
    @NotBlank
    @Size(max = 5)
    @Column(name = "iso_code", nullable = false, length = 5)
    private String isoCode;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100, unique = true)
    private String slug;

    /** International dialing prefix, e.g. "+91". */
    @Size(max = 10)
    @Column(name = "phone_code", length = 10)
    private String phoneCode;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
