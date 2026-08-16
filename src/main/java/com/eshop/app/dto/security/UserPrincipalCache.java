package com.eshop.app.dto.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.With;
import lombok.extern.jackson.Jacksonized;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Lightweight cached representation of user principal details. Positioned under the DTO package
 * namespace to automatically comply with polymorphic distributed Redis cache whitelisting.
 *
 * <p>Employs `@JsonIgnore` on authorities to bypass complex Spring Security class serialization and
 * avoid validation failures in Redis, while maintaining sub-millisecond in-memory caching locally.
 */
@Getter
@Builder
@With
@Jacksonized
public class UserPrincipalCache implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String email;
    private final Set<String> roles;

    @JsonIgnore private final Collection<GrantedAuthority> authorities;

    private final SyncStatus syncStatus;
    private final Instant lastSyncAttempt;

    /**
     * Retrieves the cached authorities, lazily rebuilding them if loaded from the distributed L2
     * cache (where they are ignored during serialization).
     *
     * @param rolePrefix Prefix to prepend to roles (e.g. "ROLE_")
     * @return Collection of GrantedAuthority
     */
    public Collection<GrantedAuthority> getAuthorities(String rolePrefix) {
        if (authorities == null) {
            if (roles == null) {
                return Collections.emptyList();
            }
            return roles.stream()
                    .filter(role -> role != null && !role.isBlank())
                    .filter(role -> !role.startsWith("default-"))
                    .filter(role -> !role.startsWith("uma_"))
                    .map(role -> new SimpleGrantedAuthority(rolePrefix + role.toUpperCase()))
                    .collect(Collectors.toUnmodifiableList());
        }
        return authorities;
    }
}
