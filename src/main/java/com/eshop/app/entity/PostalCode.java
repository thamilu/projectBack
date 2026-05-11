package com.eshop.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Master entity representing a postal code (pincode) and its locality.
 *
 * <p>Design rationale — Denormalized FKs:
 * {@code country}, {@code state}, and {@code district} are stored directly here
 * (in addition to the full hierarchy via {@code taluk}) to eliminate deep joins
 * on the performance-critical read path (pincode lookup API).
 * All three are {@code NOT NULL} to guarantee hierarchy consistency.
 *
 * <p>One pincode may map to multiple localities / post offices.
 * The unique constraint on {@code (pin_code, locality_name, post_office_name)}
 * prevents duplicate rows while still allowing multiple valid records per pincode.
 */
@Entity
@Table(
    name = "postal_codes",
    indexes = {
        @Index(name = "idx_postal_pin_code",    columnList = "pin_code"),
        @Index(name = "idx_postal_lookup",       columnList = "pin_code, locality_name"),
        @Index(name = "idx_postal_state_id",     columnList = "state_id"),
        @Index(name = "idx_postal_district_id",  columnList = "district_id"),
        @Index(name = "idx_postal_country_id",   columnList = "country_id")
    },
    uniqueConstraints = @UniqueConstraint(
        name = "uq_postal_code_locality_po",
        columnNames = {"pin_code", "locality_name", "post_office_name"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"taluk", "district", "state", "country"})
@EqualsAndHashCode(callSuper = true, exclude = {"taluk", "district", "state", "country"})
public class PostalCode extends BaseEntity {

    /** Postal code string — VARCHAR(10) supports both Indian (6-digit) and international formats. */
    @NotBlank
    @Size(max = 10)
    @Column(name = "pin_code", nullable = false, length = 10)
    private String pinCode;

    /** Village / area / locality name. Nullable — some post offices cover unnamed areas. */
    @Size(max = 150)
    @Column(name = "locality_name", length = 150)
    private String localityName;

    /** India Post office name serving this locality. */
    @Size(max = 150)
    @Column(name = "post_office_name", length = 150)
    private String postOfficeName;

    // ---- Structural FK (nullable — some pincodes may not have taluk data) ----

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taluk_id")
    private Taluk taluk;

    // ---- Denormalized FKs — NOT NULL — eliminate deep joins on read path ----

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "state_id", nullable = false)
    private State state;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
