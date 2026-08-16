package com.eshop.app.user.domain.entity;

import com.eshop.app.catalog.infrastructure.security.AttributeEncryptor;
import com.eshop.app.core.kernel.BaseEntity;
import com.eshop.app.user.domain.enums.Gender;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Past;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a user's extended profile information: personal details, contact information,
 * and associated addresses.
 *
 * <p><b>Aggregate root:</b> {@code UserProfile} is the aggregate root for {@link UserAddress}.
 * The {@code addresses} collection is managed with {@code cascade = ALL} and
 * {@code orphanRemoval = true} — adding via {@link #addAddress(UserAddress)} is the supported
 * mutation path. Do not call {@code setAddresses(...)} to replace the collection wholesale;
 * doing so can detach the JPA-managed collection from the persistence context and risks
 * unintended deletion of existing addresses on flush. This setter is retained only for
 * framework/compatibility purposes (e.g., existing service-layer code) and is discouraged for
 * new call sites.
 *
 * <p><b>PII handling:</b> {@code phone} and {@code alternatePhone} are encrypted at rest via
 * {@link AttributeEncryptor}. Once loaded, these fields hold decrypted plaintext in memory —
 * they are intentionally excluded from {@link #toString()} to prevent accidental PII exposure
 * in logs or stack traces.
 *
 * <p><b>Architecture note:</b> This entity depends on {@link AttributeEncryptor}, which resides
 * in the {@code catalog.infrastructure.security} package — a cross-module, infrastructure-into-domain
 * dependency. This is tracked as architecture debt; the encryption converter should be relocated
 * to a shared security/infrastructure module accessible without crossing bounded contexts.
 */
@Entity
@Table(name = "user_profiles", indexes = {
        @Index(name = "idx_user_profile_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "phone", "alternatePhone", "dateOfBirth", "gender"})
@EqualsAndHashCode(callSuper = true, exclude = {"user", "addresses"})
public class UserProfile extends BaseEntity {

    /**
     * The owning user of this profile (one-to-one, FK owning side).
     *
     * <p><b>Caution:</b> Reassigning this field via {@code setUser(...)} does not automatically
     * synchronize the inverse side ({@code User.userProfile}). Callers must set both sides to
     * maintain bidirectional consistency.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(length = 255)
    @Convert(converter = AttributeEncryptor.class)
    private String phone;

    @Column(name = "alternate_phone", length = 255)
    @Convert(converter = AttributeEncryptor.class)
    private String alternatePhone;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Past(message = "Date of birth must be in the past")
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "preferred_language", length = 20)
    private String preferredLanguage;

    /**
     * Addresses owned by this profile. Managed as a JPA aggregate collection
     * ({@code cascade = ALL}, {@code orphanRemoval = true}).
     *
     * <p>Initialized to an empty list via {@link Builder.Default} to prevent {@code null}
     * collections both from {@code new UserProfile()} and from {@code UserProfile.builder()}
     * (Lombok's {@code @Builder} does not honor field initializers without this annotation).
     *
     * <p>Prefer {@link #addAddress(UserAddress)} over {@code setAddresses(...)} to preserve
     * JPA collection-management semantics.
     */
    @Builder.Default
    @OneToMany(mappedBy = "userProfile", fetch = FetchType.LAZY, cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<UserAddress> addresses = new ArrayList<>();

    // ─── Behavior-driven methods ───────────────────────────────

    /**
     * Updates the user's first and last name.
     *
     * @param firstName the new first name; may be {@code null} to leave unspecified
     * @param lastName  the new last name; may be {@code null} to leave unspecified
     */
    public void updatePersonalDetails(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    /**
     * Adds an address to this profile's address collection, establishing the bidirectional
     * association ({@code address.userProfile = this}).
     *
     * @param address the address to add; must not be null
     */
    public void addAddress(UserAddress address) {
        if (this.addresses == null) {
            this.addresses = new ArrayList<>();
        }
        this.addresses.add(address);
        address.setUserProfile(this);
    }
}


