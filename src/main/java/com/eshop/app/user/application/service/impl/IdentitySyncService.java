package com.eshop.app.user.application.service.impl;

import com.eshop.app.core.exception.base.BusinessException;
import com.eshop.app.user.application.command.UserSyncCommand;
import com.eshop.app.user.application.port.in.IdentitySyncUseCase;
import com.eshop.app.user.application.service.ProfileSyncCommand;
import com.eshop.app.user.application.service.ProfileSyncService;
import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.domain.repository.UserRepository;
import com.eshop.app.user.shared.domain.enums.UserRole;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
@org.springframework.context.annotation.Primary
public class IdentitySyncService implements IdentitySyncUseCase {

    private final UserRepository userRepository;
    private final ProfileSyncService profileSyncService;

    @Override
    public Long createUserFromKeycloak(
            String keycloakId, String firstName, String lastName, String phoneNumber) {
        log.info("Creating first-time local user from Keycloak ID: {}", keycloakId);
        return syncUserFromKeycloak(
                UserSyncCommand.builder()
                        .keycloakId(keycloakId)
                        .firstName(firstName)
                        .lastName(lastName)
                        .phone(phoneNumber)
                        .emailVerified(false)
                        .build());
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Long syncUserFromKeycloak(String keycloakId, String email, String firstName, String lastName, String phoneNumber, Boolean emailVerified) {
        return syncUserFromKeycloak(
                UserSyncCommand.builder()
                        .keycloakId(keycloakId)
                        .email(email)
                        .firstName(firstName)
                        .lastName(lastName)
                        .phone(phoneNumber)
                        .emailVerified(Boolean.TRUE.equals(emailVerified))
                        .build()
        );
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Long syncUserFromKeycloak(UserSyncCommand command) {
        String keycloakId = command.getKeycloakId();
        String email = command.getEmail();
        String firstName = command.getFirstName();
        String lastName = command.getLastName();
        String phoneNumber = command.getPhone();
        boolean emailVerified = command.isEmailVerified();

        String tempId = keycloakId;
        if (tempId == null || tempId.isBlank()) {
            if (email != null && !email.isBlank()) {
                tempId =
                        java.util
                                .UUID
                                .nameUUIDFromBytes(
                                        ("fallback:email:" + email)
                                                .getBytes(java.nio.charset.StandardCharsets.UTF_8))
                                .toString();
            } else {
                log.error("Cannot sync user: Keycloak Subject (sub) is null and no email provided");
                throw new IllegalArgumentException(
                        "User identifier (sub or email) is mandatory for synchronization");
            }
            log.warn(
                    "[HARDEN] Keycloak subject is missing. Derived stable surrogate identity UUID:"
                            + " {} (for email: {})",
                    tempId,
                    email);
        }
        final String resolvedKeycloakId = tempId;

        return executeWithRetry(
                () -> {
                    Optional<User> lockedUser =
                            userRepository.findByKeycloakIdForUpdate(resolvedKeycloakId);
                    User user;
                    if (lockedUser.isPresent()) {
                        user = lockedUser.get();
                        log.debug("Found and locked existing user: {}", user.getId());
                    } else {
                        user = tryFindOrCreateUser(resolvedKeycloakId, email);
                    }

                    updateUserFromKeycloak(
                            user, email, firstName, lastName, phoneNumber, emailVerified);

                    try {
                        user = userRepository.saveAndFlush(user);
                        log.info(
                                "Successfully synced user identity for: {} (localId: {})",
                                user.getEmail(),
                                user.getId());
                        return user.getId();
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        log.warn(
                                "[HARDEN] DataIntegrityViolation for sub={}, email={}. Attempting"
                                        + " recovery read...",
                                resolvedKeycloakId,
                                email);
                        Optional<User> recovered =
                                userRepository.findByKeycloakId(resolvedKeycloakId);
                        if (recovered.isEmpty() && email != null && !email.isBlank()) {
                            recovered = userRepository.findByEmail(email).stream().findFirst();
                        }
                        if (recovered.isPresent()) {
                            log.info(
                                    "[HARDEN] Recovery successful: resolved user id={} after"
                                            + " constraint collision.",
                                    recovered.get().getId());
                            return recovered.get().getId();
                        }
                        log.error(
                                "DATA INTEGRITY ERROR: Could not recover. Constraint violation: {}."
                                        + " [sub={}, email={}]",
                                e.getMostSpecificCause().getMessage(),
                                resolvedKeycloakId,
                                email);
                        throw new BusinessException(
                                "Identity conflict: email already taken",
                                "IDENTITY_CONFLICT",
                                HttpStatus.CONFLICT);
                    }
                },
                3);
    }

    private <T> T executeWithRetry(java.util.function.Supplier<T> operation, int maxAttempts) {
        int attempt = 0;
        while (true) {
            try {
                return operation.get();
            } catch (OptimisticLockException
                    | PessimisticLockException
                    | org.springframework.dao.PessimisticLockingFailureException e) {
                attempt++;
                if (attempt >= maxAttempts) {
                    log.error(
                            "Failed sync user operation after {} attempts: {}",
                            maxAttempts,
                            e.getMessage());
                    throw new BusinessException(
                            "Unable to complete operation due to concurrent access",
                            "CONCURRENT_MODIFICATION",
                            HttpStatus.CONFLICT);
                }
                log.warn("Lock exception on attempt {}, retrying...", attempt);
                try {
                    Thread.sleep(100 * attempt); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException(
                            "Operation interrupted",
                            "INTERRUPTED",
                            HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        }
    }

    private User tryFindOrCreateUser(String keycloakId, String email) {
        if (email != null && !email.isBlank()) {
            java.util.List<User> emailUsers = userRepository.findByEmailForUpdate(email);
            if (!emailUsers.isEmpty()) {
                User user =
                        emailUsers.stream()
                                .filter(
                                        u ->
                                                u.getKeycloakId() == null
                                                        || u.getKeycloakId().equals(keycloakId))
                                .findFirst()
                                .orElse(emailUsers.getFirst());

                if (user.getKeycloakId() == null) {
                    log.error(
                            "CRITICAL INVARIANT VIOLATION: Existing user (id: {}) has null Keycloak"
                                    + " ID. Immutability invariant prevents setting it to: {}",
                            user.getId(),
                            keycloakId);
                    throw new IllegalStateException(
                            "Cannot adopt user: Keycloak ID is null and immutable");
                } else if (!user.getKeycloakId().equals(keycloakId)) {
                    log.error(
                            "CRITICAL IDENTITY DRIFT: Found user (id: {}) with email {} but"
                                + " different Keycloak ID (old: {}, new: {}). Identity links are"
                                + " immutable.",
                            user.getId(),
                            email,
                            user.getKeycloakId(),
                            keycloakId);
                    throw new IllegalStateException(
                            "Identity drift detected: Keycloak ID does not match");
                }
                return user;
            }
        }

        log.info(
                "Creating new local user record for Keycloak ID: {} (email: {})",
                keycloakId,
                email);
        return User.create(keycloakId, truncate(email, 150), UserRole.CUSTOMER);
    }

    private void updateUserFromKeycloak(
            User user,
            String email,
            String firstName,
            String lastName,
            String phoneNumber,
            Boolean emailVerified) {
        if (email != null && !email.isBlank()) {
            user.updateEmail(truncate(email, 150));
        }
        if (emailVerified != null) {
            if (emailVerified) {
                user.verifyEmail();
            } else {
                user.markEmailUnverified();
            }
        }

        log.debug("Updating user profile for user ID: {}", user.getId());
        profileSyncService.ensureProfileExists(
                user,
                new ProfileSyncCommand(
                        truncate(firstName, 100),
                        truncate(lastName, 100),
                        truncate(phoneNumber, 20),
                        null,
                        null,
                        null,
                        null));
    }

    @Override
    public void syncUserRoles(Long userId, Collection<String> keycloakRoles) {
        userRepository
                .findById(userId)
                .ifPresent(
                        user -> {
                            UserRole bestRole = determineBestRole(keycloakRoles);

                            if (user.getRole() == UserRole.SELLER
                                    && bestRole == UserRole.CUSTOMER) {
                                if (user.getSellerProfile() != null
                                        && user.getSellerProfile().getStatus()
                                                == com.eshop.app.seller.shared.domain.enums
                                                        .SellerStatus.ACTIVE) {
                                    log.info(
                                            "[HARDEN] Prevented role downgrade from SELLER to"
                                                + " CUSTOMER for user ID {} because seller profile"
                                                + " is ACTIVE",
                                            user.getId());
                                    return;
                                }
                            }

                            if (user.getRole() == UserRole.DELIVERY_AGENT
                                    && bestRole == UserRole.CUSTOMER) {
                                if (user.getDeliveryAgentProfile() != null
                                        && user.getDeliveryAgentProfile().getStatus()
                                                == com.eshop.app.user.shared.domain.enums
                                                        .DeliveryAgentStatus.ACTIVE) {
                                    log.info(
                                            "[HARDEN] Prevented role downgrade from DELIVERY_AGENT"
                                                    + " to CUSTOMER for user ID {} because delivery"
                                                    + " agent profile is ACTIVE",
                                            user.getId());
                                    return;
                                }
                            }

                            if (user.getRole() != bestRole) {
                                log.info(
                                        "Syncing role for user ID {}: {} -> {}",
                                        user.getId(),
                                        user.getRole(),
                                        bestRole);
                                user.updateRole(bestRole);
                                userRepository.save(user);
                            }
                        });
    }

    @Override
    public void syncKeycloakId(Long userId, String keycloakId) {
        userRepository
                .findById(userId)
                .ifPresent(
                        user -> {
                            if (!user.getKeycloakId().equals(keycloakId)) {
                                log.error(
                                        "CRITICAL IDENTITY DRIFT: Attempted to sync different"
                                            + " Keycloak ID for user {}. Expected: {}, Actual: {}",
                                        userId,
                                        user.getKeycloakId(),
                                        keycloakId);
                                throw new IllegalStateException(
                                        "Keycloak ID is immutable and cannot be updated");
                            }
                            log.info("Keycloak ID already synced for user {}", userId);
                        });
    }

    private UserRole determineBestRole(Collection<String> roles) {
        if (roles == null || roles.isEmpty()) return UserRole.CUSTOMER;

        Set<String> upperRoles =
                roles.stream()
                        .filter(Objects::nonNull)
                        .map(role -> role.toUpperCase())
                        .collect(Collectors.toSet());

        if (upperRoles.contains("ADMIN") || upperRoles.contains("ROLE_ADMIN"))
            return UserRole.ADMIN;
        if (upperRoles.contains("SELLER") || upperRoles.contains("ROLE_SELLER"))
            return UserRole.SELLER;
        if (upperRoles.contains("DELIVERY_AGENT") || upperRoles.contains("ROLE_DELIVERY_AGENT"))
            return UserRole.DELIVERY_AGENT;

        return UserRole.CUSTOMER;
    }

    private String truncate(String val, int length) {
        if (val == null) return null;
        return val.length() > length ? val.substring(0, length) : val;
    }
}
