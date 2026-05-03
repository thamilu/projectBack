package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;
import com.eshop.app.enums.UserRole;

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
public class User extends BaseEntity {

    @Column(name = "keycloak_id", unique = true, nullable = false)
    private String keycloakId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "email", length = 150)
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
    @Transient
    public Store getStore() {
        if (sellerProfile != null && sellerProfile.getStores() != null && !sellerProfile.getStores().isEmpty()) {
            return sellerProfile.getStores().iterator().next();
        }
        return null;
    }
}
