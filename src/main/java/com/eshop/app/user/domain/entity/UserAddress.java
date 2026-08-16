package com.eshop.app.user.domain.entity;

import com.eshop.app.core.exception.business.DomainValidationException;
import com.eshop.app.core.kernel.BaseEntity;
import com.eshop.app.location.domain.entity.PostalCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
    @Index(name = "idx_user_addresses_postal_code_id", columnList = "postal_code_id"),
    @Index(name = "idx_user_addresses_user_profile_id", columnList = "user_profile_id")
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

    // ─── Behavior-driven methods ───────────────────────────────

    public static UserAddress create(
            UserProfile userProfile,
            String addressLine1,
            String addressLine2,
            String city,
            String district,
            String taluk,
            String state,
            String pincode,
            String country,
            Boolean isDefault) {
        if (userProfile == null) {
            throw DomainValidationException.withErrorCode("userProfile must not be null", "NULL_USER_PROFILE");
        }
        UserAddress userAddress = UserAddress.builder()
                .userProfile(userProfile)
                .isDefault(isDefault != null ? isDefault : false)
                .build();
        userAddress.updateAddressDetails(
                addressLine1, addressLine2, city, district, taluk, state, pincode, country);
        return userAddress;
    }

    public void updateAddressDetails(
            String addressLine1,
            String addressLine2,
            String city,
            String district,
            String taluk,
            String state,
            String pincode,
            String country) {
        if (addressLine1 != null) {
            if (addressLine1.length() > 500) {
                throw DomainValidationException.withErrorCode("addressLine1 must not exceed 500 characters", "INVALID_ADDRESS_LINE");
            }
            this.addressLine1 = addressLine1;
        }
        if (addressLine2 != null) {
            if (addressLine2.length() > 500) {
                throw DomainValidationException.withErrorCode("addressLine2 must not exceed 500 characters", "INVALID_ADDRESS_LINE");
            }
            this.addressLine2 = addressLine2;
        }
        if (city != null) {
            if (city.length() > 100) {
                throw DomainValidationException.withErrorCode("city must not exceed 100 characters", "INVALID_CITY");
            }
            this.city = city;
        }
        if (district != null) {
            if (district.length() > 100) {
                throw DomainValidationException.withErrorCode("district must not exceed 100 characters", "INVALID_DISTRICT");
            }
            this.district = district;
        }
        if (taluk != null) {
            if (taluk.length() > 100) {
                throw DomainValidationException.withErrorCode("taluk must not exceed 100 characters", "INVALID_TALUK");
            }
            this.taluk = taluk;
        }
        if (state != null) {
            if (state.length() > 100) {
                throw DomainValidationException.withErrorCode("state must not exceed 100 characters", "INVALID_STATE");
            }
            this.state = state;
        }
        if (pincode != null) {
            if (pincode.length() > 20) {
                throw DomainValidationException.withErrorCode("pincode must not exceed 20 characters", "INVALID_PINCODE");
            }
            this.pincode = pincode;
        }
        if (country != null) {
            if (country.length() > 100) {
                throw DomainValidationException.withErrorCode("country must not exceed 100 characters", "INVALID_COUNTRY");
            }
            this.country = country;
        }
    }

    public boolean isDefaultAddress() {
        return Boolean.TRUE.equals(this.isDefault);
    }

    public void updateCoordinates(Double latitude, Double longitude) {
        if (latitude != null && (latitude < -90.0 || latitude > 90.0)) {
            throw DomainValidationException.withErrorCode("latitude must be between -90 and 90 degrees", "INVALID_LATITUDE");
        }
        if (longitude != null && (longitude < -180.0 || longitude > 180.0)) {
            throw DomainValidationException.withErrorCode("longitude must be between -180 and 180 degrees", "INVALID_LONGITUDE");
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public boolean hasCoordinates() {
        return this.latitude != null && this.longitude != null;
    }

    public void assignPostalCode(PostalCode postalCode) {
        this.postalCode = postalCode;
        if (postalCode != null) {
            this.pincode = postalCode.getPinCode();
            this.city = postalCode.getPostOfficeName();
            if (postalCode.getState() != null) {
                this.state = postalCode.getState().getName();
            }
            if (postalCode.getDistrict() != null) {
                this.district = postalCode.getDistrict().getName();
            }
            if (postalCode.getCountry() != null) {
                this.country = postalCode.getCountry().getName();
            }
        } else {
            this.pincode = null;
            this.city = null;
            this.district = null;
            this.state = null;
            this.country = null;
        }
    }

    public boolean hasPostalCode() {
        return this.postalCode != null;
    }

    public void markAsDefault() {
        this.isDefault = true;
    }

    public void unmarkAsDefault() {
        this.isDefault = false;
    }
}
