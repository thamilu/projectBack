package com.eshop.app.user.application.service.impl;

import com.eshop.app.core.exception.business.DomainValidationException;
import com.eshop.app.user.application.service.ProfileSyncCommand;
import com.eshop.app.user.application.service.ProfileSyncService;
import com.eshop.app.user.domain.entity.SellerProfile;
import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.domain.entity.UserAddress;
import com.eshop.app.user.domain.entity.UserProfile;
import com.eshop.app.user.domain.enums.Gender;
import com.eshop.app.user.domain.repository.UserRepository;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link ProfileSyncService} responsible for keeping
 * {@link UserProfile} and {@link UserAddress} state consistent with external profile sources
 * (e.g., {@link SellerProfile}).
 *
 * <p><b>Entity state precondition:</b> Both public methods accept a {@link User} instance that
 * may be either <em>managed</em> (loaded in the current transaction) or <em>detached</em>
 * (loaded in a prior transaction). The behavior of {@code userRepository.save()} differs:
 * <ul>
 *   <li>Managed entity — {@code save()} is a no-op for the entity itself; dirty-checking
 *       handles flushing. The explicit {@code save()} is retained for detached-entity safety.</li>
 *   <li>Detached entity — {@code save()} performs a {@code merge()}. Callers must ensure that
 *       {@code User.userProfile} is configured with appropriate {@code CascadeType} (PERSIST/MERGE
 *       or ALL) for new {@link UserProfile} instances to be cascaded correctly.</li>
 * </ul>
 *
 * <p><b>Collection safety:</b> {@link UserProfile#getAddresses()} is never replaced with a new
 * collection instance on an existing (potentially JPA-managed) entity. New {@link UserAddress}
 * records are added to the existing collection to preserve dirty-tracking semantics.
 *
 * <p><b>Transaction:</b> Both methods declare {@code @Transactional(rollbackFor = Exception.class)}.
 * All mutations within a single call are atomic.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultProfileSyncService implements ProfileSyncService {

    private final UserRepository userRepository;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncSellerAddressToUser(User user, SellerProfile profile) {
        if (user == null || profile == null) {
            log.warn(
                    "syncSellerAddressToUser called with null argument: "
                            + "userPresent=[{}] profilePresent=[{}]",
                    user != null,
                    profile != null);
            return;
        }

        ProfileResult profileResult = ensureAndGetProfile(user);
        UserProfile up = profileResult.profile();

        UserAddress address = resolveOrCreateDefaultAddress(up, profileResult.isNew());

        applyAddressFields(address, profile);

        userRepository.save(user);

        log.debug(
                "Seller address synchronized to UserAddress: userId=[{}] addressIsNew=[{}]",
                user.getId(),
                address.getId() == null);
    }

    /**
     * {@inheritDoc}
     *
     * @throws DomainValidationException if any field fails domain validation (e.g., invalid gender)
     * @throws IllegalArgumentException  if any field exceeds maximum allowed length
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ensureProfileExists(User user, ProfileSyncCommand command) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        if (command == null) {
            throw new IllegalArgumentException("ProfileSyncCommand must not be null");
        }

        validateCommand(command);

        UserProfile up = ensureAndGetProfile(user).profile();

        applyCommandToProfile(up, command);

        userRepository.save(user);

        log.info(
                "User profile synchronized: userId=[{}] fieldsUpdated=["
                        + "firstName={}, lastName={}, phone={}, alternatePhone={}, "
                        + "gender={}, preferredLanguage={}, dateOfBirth={}]",
                user.getId(),
                command.firstName() != null,
                command.lastName() != null,
                command.phone() != null,
                command.alternatePhone() != null,
                command.gender() != null,
                command.preferredLanguage() != null,
                command.dateOfBirth() != null);
    }

    // -------------------------------------------------------------------------
    // Private — Profile Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the existing {@link UserProfile} for the given user, or creates and attaches
     * a new one if absent.
     *
     * <p><b>Side effect:</b> If no profile exists, this method mutates {@code user} by calling
     * {@code user.setUserProfile(newProfile)}. Callers must be aware of this mutation.
     *
     * @param user the user whose profile is resolved or created
     * @return a {@link ProfileResult} containing the profile and whether it was newly created
     */
    private ProfileResult ensureAndGetProfile(User user) {
        if (user.getUserProfile() != null) {
            return new ProfileResult(user.getUserProfile(), false);
        }
        UserProfile up = new UserProfile();
        up.setUser(user);
        user.setUserProfile(up);
        log.debug("Created new UserProfile for user: userId=[{}]", user.getId());
        return new ProfileResult(up, true);
    }

    /**
     * Resolves the existing default {@link UserAddress} from the profile, or creates a new one.
     *
     * <p>For a <em>newly created</em> profile, the addresses collection is initialized as
     * an {@link ArrayList} (safe — the profile is transient and not yet managed by JPA).
     * For an <em>existing</em> profile, the existing collection reference is used as-is to
     * preserve JPA dirty-tracking semantics.
     *
     * @param up    the user profile
     * @param isNew {@code true} if this profile was just created (transient, not JPA-managed)
     * @return the existing default address, or a newly created default address
     */
    private UserAddress resolveOrCreateDefaultAddress(UserProfile up, boolean isNew) {
        if (up.getAddresses() == null) {
            if (isNew) {
                // Safe: profile is transient — no JPA-managed collection to preserve
                up.setAddresses(new ArrayList<>());
            } else {
                // Existing profile with null collection — indicates a JPA mapping issue.
                log.warn(
                        "UserProfile has null addresses collection on existing profile — "
                                + "verify @OneToMany initialization in UserProfile entity. "
                                + "userId=[{}]",
                        up.getUser() != null ? up.getUser().getId() : "[unknown]");
                up.setAddresses(new ArrayList<>());
            }
        }

        return up.getAddresses().stream()
                .filter(ua -> Boolean.TRUE.equals(ua.getIsDefault()))
                .findFirst()
                .orElseGet(() -> {
                    if (!up.getAddresses().isEmpty()) {
                        log.warn(
                                "No default address found among [{}] existing addresses — "
                                        + "creating a new default address",
                                up.getAddresses().size());
                    }
                    UserAddress ua = new UserAddress();
                    ua.setUserProfile(up);
                    ua.setIsDefault(true);
                    up.getAddresses().add(ua);
                    return ua;
                });
    }

    // -------------------------------------------------------------------------
    // Private — Field Application
    // -------------------------------------------------------------------------

    private void applyAddressFields(UserAddress address, SellerProfile profile) {
        if (profile.getAddressLine1() == null && profile.getCity() == null
                && profile.getCountry() == null) {
            log.warn(
                    "SellerProfile has no primary address fields set "
                            + "(addressLine1, city, country are all null) — "
                            + "proceeding with sync; database constraints may reject this");
        }
        address.setAddressLine1(profile.getAddressLine1());
        address.setAddressLine2(profile.getAddressLine2());
        address.setCity(profile.getCity());
        address.setDistrict(profile.getDistrict());
        address.setTaluk(profile.getTaluk());
        address.setState(profile.getState());
        address.setPincode(profile.getPincode());
        address.setCountry(profile.getCountry());
    }

    private void applyCommandToProfile(UserProfile up, ProfileSyncCommand command) {
        if (command.firstName() != null)         up.setFirstName(command.firstName());
        if (command.lastName() != null)          up.setLastName(command.lastName());
        if (command.phone() != null)             up.setPhone(command.phone());
        if (command.alternatePhone() != null)    up.setAlternatePhone(command.alternatePhone());
        if (command.gender() != null) {
            Gender parsedGender = Gender.fromString(command.gender())
                    .orElseThrow(() -> new DomainValidationException(
                            "Invalid gender option: " + command.gender(), "INVALID_GENDER"));
            up.setGender(parsedGender);
        }
        if (command.preferredLanguage() != null) up.setPreferredLanguage(command.preferredLanguage());
        if (command.dateOfBirth() != null)       up.setDateOfBirth(command.dateOfBirth());
    }

    // -------------------------------------------------------------------------
    // Private — Validation
    // -------------------------------------------------------------------------

    private void validateCommand(ProfileSyncCommand command) {
        validateMaxLength("firstName", command.firstName(), ProfileSyncCommand.MAX_NAME_LENGTH);
        validateMaxLength("lastName", command.lastName(), ProfileSyncCommand.MAX_NAME_LENGTH);
        validateMaxLength("phone", command.phone(), ProfileSyncCommand.MAX_PHONE_LENGTH);
        validateMaxLength("alternatePhone", command.alternatePhone(), ProfileSyncCommand.MAX_PHONE_LENGTH);
        validateMaxLength("preferredLanguage", command.preferredLanguage(), ProfileSyncCommand.MAX_LANGUAGE_LENGTH);
    }

    private void validateMaxLength(String fieldName, String value, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(
                    String.format("Field [%s] exceeds maximum allowed length of %d characters",
                            fieldName, maxLength));
        }
    }

    // -------------------------------------------------------------------------
    // Private — Result Carrier
    // -------------------------------------------------------------------------

    /**
     * Internal result carrier for {@link #ensureAndGetProfile(User)}, conveying both the
     * resolved {@link UserProfile} and whether it was newly created (transient) or existing
     * (potentially JPA-managed).
     *
     * @param profile the resolved or created profile
     * @param isNew   {@code true} if this profile was just created and is not yet JPA-managed
     */
    private record ProfileResult(UserProfile profile, boolean isNew) {}
}


