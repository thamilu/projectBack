package com.eshop.app.user.domain.entity;

import com.eshop.app.cart.domain.entity.Cart;
import com.eshop.app.order.domain.entity.Order;
import com.eshop.app.core.kernel.BaseEntity;
import com.eshop.app.store.domain.entity.Store;
import com.eshop.app.user.shared.domain.enums.UserRole;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = { "cart", "orders", "deliveryAgentProfile", "sellerProfile", "userProfile" })
@EqualsAndHashCode(callSuper = true, of = { "keycloakId" })
// deleted_by is intentionally left unset here: current_user() resolves to the JDBC connection's
// DB role (e.g. the app's service account), not the acting admin/user, so it cannot provide a
// meaningful audit trail. Callers that need actor attribution must use softDelete(actorId) +
// save() instead of repository.delete()/deleteById(), which sets deleted_by correctly in Java.
@SQLDelete(sql = "UPDATE users SET deleted = true, deleted_at = CURRENT_TIMESTAMP, " +
        "version = version + 1 WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
public class User extends BaseEntity {

    @Column(name = "keycloak_id", unique = true, nullable = false)
    private String keycloakId;

    @Column(name = "email", length = 255, unique = true, nullable = false)
    @jakarta.persistence.Convert(converter = com.eshop.app.catalog.infrastructure.security.AttributeEncryptor.class)
    private String email;

    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserProfile userProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private SellerProfile sellerProfile;

    // Assuming Cart and Order entities exist but I don't want to import them if not
    // needed.
    // They are used in ToString exclude so they must exist in the class.

    // I need imports for these too potentially?
    // User.java in Step 113 imports: jakarta.persistence.*, lombok.*,
    // java.util.HashSet, java.util.Set.
    // It does NOT import Cart, Order, etc.
    // If they are in same package com.eshop.app.entity, no import needed.

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Cart cart;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private Set<Order> orders;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private DeliveryAgentProfile deliveryAgentProfile;

    @Column(name = "two_factor_enabled")
    @Builder.Default
    private Boolean twoFactorEnabled = false;

    @Column(name = "two_factor_secret")
    private String twoFactorSecret;

    // Simplified getters/setters via Lombok @Getter/@Setter

    /**
     * Backward compatibility helper to get the user's store.
     * Navigates through SellerProfile to first available store.
     */
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    // ─── Behavior-driven methods ───────────────────────────────

    public static User create(String keycloakId, String email, UserRole role) {
        return User.builder()
                .keycloakId(keycloakId)
                .email(email)
                .role(role)
                .emailVerified(false)
                .twoFactorEnabled(false)
                .deleted(false)
                .build();
    }

    public void updateRole(UserRole role) {
        this.role = role;
    }

    public void updateEmail(String email) {
        this.email = email;
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public void markEmailUnverified() {
        this.emailVerified = false;
    }

    public void activate() {
        this.deleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }

    public void softDelete(String deletedBy) {
        this.deleted = true;
        this.deletedAt = java.time.LocalDateTime.now();
        this.deletedBy = deletedBy;
    }

    @Transient
    public Store getStore() {
        if (sellerProfile != null && sellerProfile.getStores() != null && !sellerProfile.getStores().isEmpty()) {
            return sellerProfile.getStores().iterator().next();
        }
        return null;
    }
}

