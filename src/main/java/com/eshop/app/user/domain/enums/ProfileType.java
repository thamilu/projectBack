package com.eshop.app.user.domain.enums;

import com.eshop.app.user.shared.domain.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * Discriminator for user profile variants in the e-shop system.
 *
 * <p>Defines the specialized profile types that extend the base {@code User} entity. Each profile
 * type represents a distinct user role with specific capabilities, verification requirements, and
 * business rules.
 *
 * <p><b>Design Rationale:</b>
 *
 * <ul>
 *   <li>Extracted from {@code User} entity to respect Single Responsibility Principle
 *   <li>Enables polymorphic profile management via strategy pattern
 *   <li>Supports role-based access control integration
 *   <li>Facilitates profile-specific feature flags and business rules
 * </ul>
 *
 * <h3>Architecture:</h3>
 *
 * <pre>
 * User (Base Entity)
 *   ├─ UserProfile      (ProfileType.USER)
 *   ├─ SellerProfile    (ProfileType.SELLER)
 *   └─ DeliveryAgentProfile (ProfileType.DELIVERY_AGENT)
 * </pre>
 *
 * <h3>Usage Examples:</h3>
 *
 * <pre>{@code
 * // Check if user can sell products
 * if (user.hasProfile(ProfileType.SELLER) && profileType.canSellProducts()) {
 *     // Allow store creation
 * }
 *
 * // Get profile display name for UI
 * String label = ProfileType.SELLER.getDisplayName(); // "Seller"
 *
 * // Check verification requirement
 * if (profileType.requiresVerification()) {
 *     // Initiate verification flow
 * }
 *
 * // Parse from API input
 * Optional<ProfileType> type = ProfileType.fromString("delivery agent");
 *
 * // Get associated role
 * UserRole role = ProfileType.SELLER.getPrimaryRole(); // UserRole.SELLER
 * }</pre>
 *
 * <h3>Database Mapping:</h3>
 *
 * <pre>{@code
 * // JPA entity discriminator
 * @Enumerated(EnumType.STRING)
 * @Column(name = "profile_type")
 * private ProfileType profileType;
 * }</pre>
 *
 * @author Your Team
 * @version 2.0
 * @since 1.0
 * @see com.eshop.app.user.domain.entity.User
 * @see com.eshop.app.user.domain.entity.UserProfile
 * @see com.eshop.app.user.domain.entity.SellerProfile
 * @see com.eshop.app.user.domain.entity.DeliveryAgentProfile
 */
@Getter
public enum ProfileType {

    // ========================================================================
    // Enum Constants (Organized by Business Domain)
    // ========================================================================

    /**
     * Standard customer profile for regular shoppers.
     *
     * <p><b>Capabilities:</b>
     *
     * <ul>
     *   <li>Browse and purchase products
     *   <li>Manage orders and returns
     *   <li>Save wishlists and favorites
     *   <li>Write product reviews
     * </ul>
     *
     * <p><b>Verification:</b> Email verification only (optional for browsing, required for
     * checkout)
     *
     * <p><b>Associated Role:</b> {@link UserRole#CUSTOMER}
     */
    USER(
            "U",
            "Customer",
            "Standard customer profile for shopping",
            UserRole.CUSTOMER,
            false, // No strict verification required
            false, // Cannot sell products
            false, // Cannot deliver orders
            false // No business details needed
            ),

    /**
     * Marketplace seller profile for vendors and merchants.
     *
     * <p><b>Capabilities:</b>
     *
     * <ul>
     *   <li>Create and manage online stores
     *   <li>List products for sale
     *   <li>Process customer orders
     *   <li>Access seller dashboard and analytics
     *   <li>Manage inventory and pricing
     * </ul>
     *
     * <p><b>Verification:</b> Mandatory (business documents, tax ID, bank account verification)
     *
     * <p><b>Associated Role:</b> {@link UserRole#SELLER}
     *
     * <p><b>Business Rules:</b>
     *
     * <ul>
     *   <li>Requires approved {@code SellerProfile}
     *   <li>Must maintain minimum seller rating
     *   <li>Subject to commission fees on sales
     * </ul>
     */
    SELLER(
            "S",
            "Seller",
            "Marketplace vendor with product listings",
            UserRole.SELLER,
            true, // Strict verification required
            true, // Can sell products
            false, // Cannot deliver orders (unless also delivery agent)
            true // Business details required
            ),

    /**
     * Last-mile delivery agent profile for order fulfillment.
     *
     * <p><b>Capabilities:</b>
     *
     * <ul>
     *   <li>Accept delivery assignments
     *   <li>Update delivery status in real-time
     *   <li>Access delivery route optimization
     *   <li>Collect cash on delivery (COD)
     *   <li>Track earnings and payouts
     * </ul>
     *
     * <p><b>Verification:</b> Mandatory (driving license, vehicle registration, background check)
     *
     * <p><b>Associated Role:</b> {@link UserRole#DELIVERY_AGENT}
     *
     * <p><b>Business Rules:</b>
     *
     * <ul>
     *   <li>Requires approved {@code DeliveryAgentProfile}
     *   <li>Must maintain vehicle insurance
     *   <li>Subject to delivery performance metrics
     *   <li>Zone-based assignment restrictions
     * </ul>
     */
    DELIVERY_AGENT(
            "D",
            "Delivery Agent",
            "Last-mile delivery partner for order fulfillment",
            UserRole.DELIVERY_AGENT,
            true, // Strict verification required
            false, // Cannot sell products (unless also seller)
            true, // Can deliver orders
            true // Business details required (vehicle, license)
            );

    // ========================================================================
    // Static Cache for Fast Lookups (DRY Principle)
    // ========================================================================

    /** Cache for code-based lookups (e.g., "S" → SELLER). */
    private static final Map<String, ProfileType> CODE_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toUnmodifiableMap(p -> p.getCode(), Function.identity()));

    /** Cache for case-insensitive name lookups (e.g., "seller" → SELLER). */
    private static final Map<String, ProfileType> NAME_MAP =
            Arrays.stream(values())
                    .collect(
                            Collectors.toUnmodifiableMap(
                                    p -> p.name().toLowerCase(), Function.identity()));

    /** Cache for display name lookups (e.g., "Delivery Agent" → DELIVERY_AGENT). */
    private static final Map<String, ProfileType> DISPLAY_NAME_MAP =
            Arrays.stream(values())
                    .collect(
                            Collectors.toUnmodifiableMap(
                                    p -> p.getDisplayName().toLowerCase(), Function.identity()));

    // ========================================================================
    // Instance Fields
    // ========================================================================

    /**
     * Short code for database/API usage (e.g., "U", "S", "D").
     *
     * <p>Optimized for storage efficiency and API contracts.
     */
    private final String code;

    /**
     * Human-readable display name for UI (e.g., "Customer", "Seller", "Delivery Agent").
     *
     * <p>Should be used in dropdowns, dashboards, and user-facing displays.
     */
    @JsonValue // Jackson serializes using this field
    private final String displayName;

    /** Detailed description for documentation and tooltips. */
    private final String description;

    /**
     * Primary role associated with this profile type.
     *
     * <p>Used for role-based access control (RBAC) integration.
     */
    private final UserRole primaryRole;

    /**
     * Indicates if strict verification is required before profile activation.
     *
     * <p>Verification may include: document upload, KYC checks, background verification.
     */
    private final boolean requiresVerification;

    /** Indicates if this profile type can list and sell products. */
    private final boolean canSellProducts;

    /** Indicates if this profile type can accept and fulfill delivery orders. */
    private final boolean canDeliverOrders;

    /**
     * Indicates if business/professional details are required.
     *
     * <p>Examples: Tax ID, business license, vehicle registration, bank account.
     */
    private final boolean requiresBusinessDetails;

    // ========================================================================
    // Constructor
    // ========================================================================

    /**
     * Private constructor for enum constants.
     *
     * @param code short code for database/API
     * @param displayName human-readable name for UI
     * @param description detailed description
     * @param primaryRole associated role for RBAC
     * @param requiresVerification whether verification is mandatory
     * @param canSellProducts whether can sell products
     * @param canDeliverOrders whether can deliver orders
     * @param requiresBusinessDetails whether business details are required
     */
    ProfileType(
            String code,
            String displayName,
            String description,
            UserRole primaryRole,
            boolean requiresVerification,
            boolean canSellProducts,
            boolean canDeliverOrders,
            boolean requiresBusinessDetails) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
        this.primaryRole = primaryRole;
        this.requiresVerification = requiresVerification;
        this.canSellProducts = canSellProducts;
        this.canDeliverOrders = canDeliverOrders;
        this.requiresBusinessDetails = requiresBusinessDetails;
    }

    // ========================================================================
    // Parsing Methods (DRY - Reusable Across Application)
    // ========================================================================

    /**
     * Parses profile type from string input (case-insensitive).
     *
     * <p>Supports multiple input formats:
     *
     * <ul>
     *   <li>Enum name: "SELLER", "seller", "Seller"
     *   <li>Display name: "Delivery Agent", "delivery agent"
     *   <li>Code: "U", "S", "D"
     * </ul>
     *
     * @param value string representation of profile type
     * @return Optional containing the matched ProfileType, or empty if not found
     */
    @JsonCreator // Jackson uses this for deserialization
    public static Optional<ProfileType> fromString(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        String normalized = value.trim();

        // Try exact code match first (fastest)
        ProfileType byCode = CODE_MAP.get(normalized.toUpperCase());
        if (byCode != null) {
            return Optional.of(byCode);
        }

        // Try enum name match
        ProfileType byName = NAME_MAP.get(normalized.toLowerCase());
        if (byName != null) {
            return Optional.of(byName);
        }

        // Try display name match
        ProfileType byDisplayName = DISPLAY_NAME_MAP.get(normalized.toLowerCase());
        if (byDisplayName != null) {
            return Optional.of(byDisplayName);
        }

        return Optional.empty();
    }

    /**
     * Parses profile type from short code (e.g., "U", "S", "D").
     *
     * @param code short code
     * @return Optional containing the matched ProfileType, or empty if not found
     */
    public static Optional<ProfileType> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(CODE_MAP.get(code.trim().toUpperCase()));
    }

    /**
     * Parses profile type from string with fallback to default.
     *
     * @param value string representation
     * @param defaultType fallback if parsing fails
     * @return parsed ProfileType or default
     */
    public static ProfileType fromStringOrDefault(String value, ProfileType defaultType) {
        return fromString(value).orElse(defaultType);
    }

    /**
     * Parses profile type from string or throws exception.
     *
     * @param value string representation
     * @return parsed ProfileType
     * @throws IllegalArgumentException if value is invalid
     */
    public static ProfileType fromStringOrThrow(String value) {
        return fromString(value)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        String.format(
                                                "Invalid profile type: '%s'. Valid values: %s",
                                                value, Arrays.toString(values()))));
    }

    // ========================================================================
    // Business Logic - Capability Queries
    // ========================================================================

    /**
     * Checks if this profile type is for a customer/buyer.
     *
     * @return true if USER profile
     */
    public boolean isCustomer() {
        return this == USER;
    }

    /**
     * Checks if this profile type is for a seller/vendor.
     *
     * @return true if SELLER profile
     */
    public boolean isSeller() {
        return this == SELLER;
    }

    /**
     * Checks if this profile type is for a delivery agent.
     *
     * @return true if DELIVERY_AGENT profile
     */
    public boolean isDeliveryAgent() {
        return this == DELIVERY_AGENT;
    }

    /**
     * Checks if this profile type requires verification before activation.
     *
     * @return true if verification is mandatory
     */
    public boolean requiresVerification() {
        return requiresVerification;
    }

    /**
     * Checks if this profile type has marketplace seller capabilities.
     *
     * @return true if can list and sell products
     */
    public boolean canSellProducts() {
        return canSellProducts;
    }

    /**
     * Checks if this profile type has delivery fulfillment capabilities.
     *
     * @return true if can accept and deliver orders
     */
    public boolean canDeliverOrders() {
        return canDeliverOrders;
    }

    /**
     * Checks if business/professional details are required.
     *
     * @return true if business details mandatory
     */
    public boolean requiresBusinessDetails() {
        return requiresBusinessDetails;
    }

    /**
     * Checks if this profile type has any commercial capabilities.
     *
     * <p>Commercial capabilities include selling products or delivering orders.
     *
     * @return true if SELLER or DELIVERY_AGENT
     */
    public boolean isCommercialProfile() {
        return canSellProducts || canDeliverOrders;
    }

    /**
     * Gets the set of permissions associated with this profile type.
     *
     * @return set of permission strings for authorization
     */
    public Set<String> getPermissions() {
        return switch (this) {
            case USER -> Set.of("ORDER:CREATE", "ORDER:VIEW", "REVIEW:CREATE");
            case SELLER ->
                    Set.of(
                            "STORE:CREATE",
                            "STORE:MANAGE",
                            "PRODUCT:CREATE",
                            "PRODUCT:MANAGE",
                            "ORDER:VIEW",
                            "ORDER:FULFILL",
                            "ANALYTICS:VIEW");
            case DELIVERY_AGENT ->
                    Set.of(
                            "DELIVERY:ACCEPT",
                            "DELIVERY:UPDATE",
                            "DELIVERY:COMPLETE",
                            "ROUTE:VIEW",
                            "EARNINGS:VIEW");
        };
    }

    // ========================================================================
    // Display Methods (UI Support)
    // ========================================================================

    /**
     * Gets abbreviated display text (e.g., for badges).
     *
     * @return short code
     */
    public String getAbbreviation() {
        return code;
    }

    /**
     * Gets formatted display with code (e.g., "Seller (S)").
     *
     * @return display name with code
     */
    public String getFullDisplay() {
        return String.format("%s (%s)", displayName, code);
    }

    /**
     * Gets icon name for UI rendering (frontend integration).
     *
     * @return icon identifier
     */
    public String getIconName() {
        return switch (this) {
            case USER -> "user";
            case SELLER -> "store";
            case DELIVERY_AGENT -> "delivery";
        };
    }

    /**
     * Gets dashboard route for this profile type.
     *
     * @return URL path to profile-specific dashboard
     */
    public String getDashboardRoute() {
        return switch (this) {
            case USER -> "/customer/dashboard";
            case SELLER -> "/seller/dashboard";
            case DELIVERY_AGENT -> "/delivery/dashboard";
        };
    }

    /**
     * Returns display name for string representation.
     *
     * @return display name
     */
    @Override
    public String toString() {
        return displayName;
    }

    // ========================================================================
    // Validation Methods
    // ========================================================================

    /**
     * Validates if a string represents a valid profile type.
     *
     * @param value string to validate
     * @return true if valid profile type value
     */
    public static boolean isValid(String value) {
        return fromString(value).isPresent();
    }

    /**
     * Validates if a code represents a valid profile type.
     *
     * @param code code to validate
     * @return true if valid profile type code
     */
    public static boolean isValidCode(String code) {
        return fromCode(code).isPresent();
    }

    /**
     * Gets all profile types that require verification.
     *
     * @return array of profile types requiring verification
     */
    public static ProfileType[] getVerificationRequiredTypes() {
        return Arrays.stream(values())
                .filter(p -> p != null && p.requiresVerification())
                .toArray(size -> new ProfileType[size]);
    }

    /**
     * Gets all commercial profile types (seller/delivery).
     *
     * @return array of commercial profile types
     */
    public static ProfileType[] getCommercialTypes() {
        return Arrays.stream(values())
                .filter(p -> p != null && p.isCommercialProfile())
                .toArray(size -> new ProfileType[size]);
    }
}
