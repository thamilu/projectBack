package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a saved user address.
 *
 * <p>Migration strategy (fresh project — no legacy backfill required):
 * <ul>
 *   <li>{@code postalCode} FK is the canonical address reference for all new addresses.</li>
 *   <li>Free-text fields ({@code city}, {@code district}, {@code state}, {@code country},
 *       {@code pincode}) are retained for the current phase and can be removed in a
 *       future migration once the address flow is fully migrated to pincode-first.</li>
 * </ul>
 */
@Entity
@Table(name = "user_addresses", indexes = {
    @Index(name = "idx_user_addresses_postal_code_id", columnList = "postal_code_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"userProfile", "postalCode"})
@EqualsAndHashCode(callSuper = true, exclude = {"userProfile", "postalCode"})
public class UserAddress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(name = "address_line1", length = 500)
    private String addressLine1;

    @Column(name = "address_line2", length = 500)
    private String addressLine2;

    // ---- Normalized address reference (preferred for all new addresses) ----

    /**
     * Reference to the master postal code. Populated by the pincode-first address flow.
     * Nullable to maintain compatibility with any pre-existing rows.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postal_code_id")
    private PostalCode postalCode;

    // ---- Legacy free-text fields (kept for migration safety) ----

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String district;

    @Column(length = 100)
    private String taluk;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String country;

    @Column(length = 20)
    private String pincode;

    // ---- Geo coordinates (future delivery / map integration) ----

    private Double latitude;
    private Double longitude;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;
}
