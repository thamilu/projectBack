package com.eshop.app.user.domain.event;

import com.eshop.app.core.events.contract.DomainEvent;
import com.eshop.app.core.exception.business.DomainValidationException;
import com.eshop.app.user.shared.domain.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;

/**
 * Domain event published when a user's primary role is changed in the system.
 *
 * <p><b>Business Meaning:</b> This event signals a critical change in user authorization level and
 * platform capabilities. Role changes directly impact:
 *
 * <ul>
 *   <li>Access control and permissions
 *   <li>Feature availability (marketplace selling, delivery fulfillment)
 *   <li>Payment processing (seller commission, driver payouts)
 *   <li>Verification requirements (new profiles may need verification)
 *   <li>Legal/compliance obligations (seller agreements, delivery regulations)
 * </ul>
 *
 * <p><b>Event Guarantees:</b>
 *
 * <ul>
 *   <li>Published ONLY after successful role transition in the aggregate
 *   <li>Old and new roles are always different (idempotent — duplicate changes ignored)
 *   <li>User exists and is in good standing
 *   <li>Event is immutable after creation
 *   <li>Event timestamp is absolute (UTC)
 *   <li>Event ID ensures deduplication in event store
 * </ul>
 *
 * <p><b>Valid Role Transitions:</b>
 *
 * <pre>
 * CUSTOMER ──────┬──&gt; SELLER              (enable marketplace selling)
 *                └──&gt; DELIVERY_AGENT      (enable delivery fulfillment)
 *
 * SELLER ────────┬──&gt; CUSTOMER            (revoke marketplace access)
 *                └──&gt; DELIVERY_AGENT      (add delivery capability)
 *
 * DELIVERY_AGENT ┬──&gt; CUSTOMER            (revoke delivery access)
 *                └──&gt; SELLER              (add marketplace capability)
 *
 * Invalid Transitions:
 * - Role to itself (not published — idempotent guard in User aggregate)
 * - ADMIN ↔ Any role (special handling required outside this event)
 * </pre>
 *
 * <p><b>Event Sequence (Seller Enablement Example):</b>
 *
 * <pre>
 * UserCreatedEvent (1. User account created)
 *   ↓
 * EmailVerifiedEvent (2. Email verified)
 *   ↓
 * UserRoleUpdatedEvent CUSTOMER→SELLER (3. This event)
 *   ↓
 * SellerProfileCreatedEvent (4. Seller profile created)
 *   ↓
 * SellerActivatedEvent (5. Seller activated and visible)
 * </pre>
 *
 * <p><b>Downstream Consumers &amp; Impact:</b>
 *
 * <ul>
 *   <li><b>PermissionService:</b> Recalculate user permissions based on new role
 *   <li><b>ProfileService:</b> Create/activate related profiles (SellerProfile,
 *       DeliveryAgentProfile)
 *   <li><b>PaymentService:</b> Update commission/payout rules for new role
 *   <li><b>NotificationService:</b> Send role change confirmation and onboarding info
 *   <li><b>ComplianceService:</b> Apply new regulatory requirements
 *   <li><b>AuditService:</b> Log critical authorization change for compliance
 *   <li><b>AnalyticsService:</b> Track user journey and role adoption metrics
 *   <li><b>SecurityService:</b> Update security policies for new role
 * </ul>
 *
 * <p><b>Usage Examples:</b>
 *
 * <pre>{@code
 * // Creating event with full context (in service layer)
 * UserRoleUpdatedEvent event = UserRoleUpdatedEvent.create(
 *     userId,                              // User ID
 *     UserRole.CUSTOMER,                   // Old role
 *     UserRole.SELLER,                     // New role
 *     "req-123",                           // Correlation ID
 *     "cause-456",                         // Causation ID
 *     RoleChangeSource.ADMIN_APPROVAL,     // How was it approved
 *     "Admin approved seller onboarding"   // Why
 * );
 *
 * // Publishing event
 * eventPublisher.publish(event);
 *
 * // Consuming event
 * @EventListener
 * public void onUserRoleUpdated(UserRoleUpdatedEvent event) {
 *     permissionService.updatePermissions(event.getUserId(), event.getNewRole());
 *
 *     if (event.isSellerRoleAdded()) {
 *         profileService.createSellerProfile(event.getUserId());
 *     }
 *     notificationService.sendRoleChangeEmail(event.getUserId(), event);
 * }
 *
 * // Role transition analysis
 * if (event.isElevatingRole()) {
 *     auditService.logEscalation(event);
 * } else if (event.isRevokingRole()) {
 *     auditService.logRevocation(event);
 * }
 * }</pre>
 *
 * <h3>Event Sourcing Support:</h3>
 *
 * <p>This event is designed for event sourcing with support for:
 *
 * <ul>
 *   <li>Event replay for role history reconstruction
 *   <li>Event versioning (current version: 1)
 *   <li>Distributed tracing (correlation/causation IDs)
 *   <li>Idempotent processing (event ID)
 *   <li>Time-based projections (occurred timestamp)
 *   <li>Audit trail (change reason and approval)
 * </ul>
 *
 * @author Your Team
 * @version 1.0
 * @since 1.0
 * @see DomainEvent
 * @see EmailVerifiedEvent
 * @see SellerActivatedEvent
 */
@Getter
public final class UserRoleUpdatedEvent implements DomainEvent {

    // ========================================================================
    // Constants
    // ========================================================================

    /**
     * Event type identifier for this domain event. Used for polymorphic event handling and event
     * store querying.
     */
    public static final String EVENT_TYPE = "user.role.updated";

    /**
     * Current event schema version. Incremented when event structure changes (for schema
     * evolution).
     */
    public static final int EVENT_VERSION = 1;

    /**
     * Valid role transitions map. Maps each source role to the set of valid target roles. Used by
     * {@link #isValidTransition()} to enforce business rules.
     */
    private static final Map<UserRole, Set<UserRole>> VALID_TRANSITIONS =
            Map.ofEntries(
                    Map.entry(
                            UserRole.CUSTOMER,
                            EnumSet.of(UserRole.SELLER, UserRole.DELIVERY_AGENT)),
                    Map.entry(
                            UserRole.SELLER,
                            EnumSet.of(UserRole.CUSTOMER, UserRole.DELIVERY_AGENT)),
                    Map.entry(
                            UserRole.DELIVERY_AGENT,
                            EnumSet.of(UserRole.CUSTOMER, UserRole.SELLER)));

    // ========================================================================
    // Event Identity & Metadata
    // ========================================================================

    /**
     * Unique identifier for this event. Used for deduplication and idempotent processing.
     *
     * <p>Generated once at creation time, never changes.
     */
    @NotBlank(message = "Event ID is required")
    @JsonProperty("eventId")
    private final String eventId;

    /** Type identifier for this event. Enables polymorphic handling of different event types. */
    @NotBlank(message = "Event type is required")
    @JsonProperty("type")
    private final String eventType;

    /** Schema version of this event. Enables handling of event structure evolution. */
    @Positive(message = "Event version must be positive")
    @JsonProperty("eventVersion")
    private final int eventVersion;

    /**
     * Timestamp when event occurred (UTC). Absolute time for event ordering and time-based queries.
     */
    @NotNull(message = "Event occurred timestamp is required")
    @JsonProperty("occurredAt")
    private final Instant occurredAt;

    // ========================================================================
    // Distributed Tracing & Correlation
    // ========================================================================

    /**
     * Correlation ID for distributed tracing. Links related events across multiple
     * services/transactions.
     *
     * <p>Example: All events from a single role-change workflow share the same correlation ID.
     */
    @NotBlank(message = "Correlation ID is required")
    @JsonProperty("correlationId")
    private final String correlationId;

    /**
     * Causation ID for event causality tracking. Identifies the event that caused this event.
     *
     * <p>Example: SellerApprovalEvent → UserRoleUpdatedEvent
     */
    @NotBlank(message = "Causation ID is required")
    @JsonProperty("causationId")
    private final String causationId;

    // ========================================================================
    // Event Data — User & Role Information
    // ========================================================================

    /** User ID whose role was changed. Foreign key to {@code User} aggregate. */
    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    @JsonProperty("userId")
    private final Long userId;

    /**
     * User's previous role before the change. Enables tracking role history and reverting changes
     * if needed.
     */
    @NotNull(message = "Old role is required")
    @JsonProperty("oldRole")
    private final UserRole oldRole;

    /** User's new role after the change. The new authorization level and capabilities. */
    @NotNull(message = "New role is required")
    @JsonProperty("newRole")
    private final UserRole newRole;

    // ========================================================================
    // Event Data — Change Context (Audit Trail)
    // ========================================================================

    /**
     * Source/trigger of the role change. Tracks how the change was initiated and authorized. For
     * audit trail and compliance documentation.
     */
    @NotNull(message = "Role change source is required")
    @JsonProperty("changeSource")
    private final RoleChangeSource changeSource;

    /** Reason/justification for the role change. For audit trail and compliance documentation. */
    @NotBlank(message = "Change reason is required")
    @JsonProperty("changeReason")
    private final String changeReason;

    // ========================================================================
    // Constructor & Factory Methods
    // ========================================================================

    /**
     * Factory method to create a fully-contextualized UserRoleUpdatedEvent.
     *
     * @param userId user ID (must be positive)
     * @param oldRole previous role (must not be null)
     * @param newRole new role (must not be null, must differ from oldRole)
     * @param correlationId correlation ID for distributed tracing
     * @param causationId causation ID (event that caused this)
     * @param changeSource how the change was triggered
     * @param changeReason why the change was made
     * @return validated UserRoleUpdatedEvent instance
     * @throws DomainValidationException if validation fails
     */
    public static UserRoleUpdatedEvent create(
            Long userId,
            UserRole oldRole,
            UserRole newRole,
            String correlationId,
            String causationId,
            RoleChangeSource changeSource,
            String changeReason) {

        return builder()
                .userId(userId)
                .oldRole(oldRole)
                .newRole(newRole)
                .correlationId(correlationId)
                .causationId(causationId)
                .changeSource(changeSource)
                .changeReason(changeReason)
                .build();
    }

    /**
     * Factory method with minimal required parameters. Uses auto-generated correlation/causation
     * IDs and default change context.
     *
     * @param userId user ID
     * @param oldRole previous role
     * @param newRole new role
     * @return validated UserRoleUpdatedEvent instance
     */
    public static UserRoleUpdatedEvent createSimple(
            Long userId, UserRole oldRole, UserRole newRole) {
        return builder().userId(userId).oldRole(oldRole).newRole(newRole).build();
    }

    /**
     * Legacy constructor matching the original record signature. Enables backward compatibility
     * with direct constructor calls in existing aggregate code (e.g., {@code User.updateRole()}).
     *
     * <p>Generates tracing IDs automatically and uses default change context.
     *
     * @param userId user ID
     * @param oldRole previous role
     * @param newRole new role
     */
    public UserRoleUpdatedEvent(Long userId, UserRole oldRole, UserRole newRole) {
        this(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                EVENT_VERSION,
                Instant.now(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                userId,
                oldRole,
                newRole,
                RoleChangeSource.SYSTEM_INTERNAL,
                "Role updated via aggregate domain behavior");
    }

    /** Private all-args constructor — validated entry point (use builder or factory methods). */
    @JsonCreator
    private UserRoleUpdatedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("type") String eventType,
            @JsonProperty("eventVersion") int eventVersion,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("causationId") String causationId,
            @JsonProperty("userId") Long userId,
            @JsonProperty("oldRole") UserRole oldRole,
            @JsonProperty("newRole") UserRole newRole,
            @JsonProperty("changeSource") RoleChangeSource changeSource,
            @JsonProperty("changeReason") String changeReason) {

        this.eventId =
                eventId != null && !eventId.isBlank() ? eventId : UUID.randomUUID().toString();
        this.eventType = eventType != null && !eventType.isBlank() ? eventType : EVENT_TYPE;
        this.eventVersion = eventVersion > 0 ? eventVersion : EVENT_VERSION;
        this.occurredAt = occurredAt != null ? occurredAt : Instant.now();
        this.correlationId =
                Validator.requireNonBlank(
                        correlationId,
                        "correlationId",
                        "Correlation ID is required for distributed tracing");
        this.causationId =
                Validator.requireNonBlank(
                        causationId,
                        "causationId",
                        "Causation ID is required to track event causality");
        this.userId =
                Validator.requirePositive(
                        userId, "userId", "User ID is required and must be positive");
        this.oldRole = Validator.requireNonNull(oldRole, "oldRole", "Old role is required");
        this.newRole = Validator.requireNonNull(newRole, "newRole", "New role is required");
        this.changeSource = changeSource != null ? changeSource : RoleChangeSource.SYSTEM_INTERNAL;
        this.changeReason =
                (changeReason != null && !changeReason.isBlank())
                        ? changeReason.trim()
                        : "Role updated via aggregate domain behavior";
    }

    // ========================================================================
    // Builder & toBuilder
    // ========================================================================

    /** Returns a new {@link Builder} for constructing a {@code UserRoleUpdatedEvent}. */
    public static Builder builder() {
        return new Builder();
    }

    /** Returns a pre-populated {@link Builder} seeded from this instance (copy-constructor). */
    public Builder toBuilder() {
        return new Builder()
                .eventId(this.eventId)
                .eventType(this.eventType)
                .eventVersion(this.eventVersion)
                .occurredAt(this.occurredAt)
                .correlationId(this.correlationId)
                .causationId(this.causationId)
                .userId(this.userId)
                .oldRole(this.oldRole)
                .newRole(this.newRole)
                .changeSource(this.changeSource)
                .changeReason(this.changeReason);
    }

    /** Fluent builder for {@link UserRoleUpdatedEvent}. Validation occurs in the constructor. */
    public static final class Builder {

        private String eventId = UUID.randomUUID().toString();
        private String eventType = EVENT_TYPE;
        private int eventVersion = EVENT_VERSION;
        private Instant occurredAt = Instant.now();
        private String correlationId = UUID.randomUUID().toString();
        private String causationId = UUID.randomUUID().toString();
        private Long userId;
        private UserRole oldRole;
        private UserRole newRole;
        private RoleChangeSource changeSource = RoleChangeSource.SYSTEM_INTERNAL;
        private String changeReason = "Role updated via aggregate domain behavior";

        private Builder() {}

        public Builder eventId(String val) {
            this.eventId = val;
            return this;
        }

        public Builder eventType(String val) {
            this.eventType = val;
            return this;
        }

        public Builder eventVersion(int val) {
            this.eventVersion = val;
            return this;
        }

        public Builder occurredAt(Instant val) {
            this.occurredAt = val;
            return this;
        }

        public Builder correlationId(String val) {
            this.correlationId = val;
            return this;
        }

        public Builder causationId(String val) {
            this.causationId = val;
            return this;
        }

        public Builder userId(Long val) {
            this.userId = val;
            return this;
        }

        public Builder oldRole(UserRole val) {
            this.oldRole = val;
            return this;
        }

        public Builder newRole(UserRole val) {
            this.newRole = val;
            return this;
        }

        public Builder changeSource(RoleChangeSource val) {
            this.changeSource = val;
            return this;
        }

        public Builder changeReason(String val) {
            this.changeReason = val;
            return this;
        }

        /** Builds and validates the event. All validation happens once, in the constructor. */
        public UserRoleUpdatedEvent build() {
            return new UserRoleUpdatedEvent(
                    eventId,
                    eventType,
                    eventVersion,
                    occurredAt,
                    correlationId,
                    causationId,
                    userId,
                    oldRole,
                    newRole,
                    changeSource,
                    changeReason);
        }
    }

    // =========================================================================
    // Inner Validator (DRY - Single place for all validation logic)
    // =========================================================================

    private static final class Validator {

        private Validator() {}

        static String requireNonBlank(String value, String field, String message) {
            return EventValidator.requireNonBlank(value, field, message);
        }

        static Long requirePositive(Long value, String field, String message) {
            return EventValidator.requirePositiveLong(value, field, message);
        }

        static <T> T requireNonNull(T value, String field, String message) {
            return EventValidator.requireNonNull(value, field, message);
        }
    }

    // ========================================================================
    // DomainEvent Implementation
    // ========================================================================

    /**
     * Gets correlation ID for distributed tracing.
     *
     * @return correlation ID
     */
    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Gets epoch millisecond timestamp when event occurred.
     *
     * @return occurred timestamp in epoch millis
     */
    @Override
    public long getOccurredAt() {
        return occurredAt.toEpochMilli();
    }

    /**
     * Gets event type identifier.
     *
     * @return event type
     */
    @Override
    public String getEventType() {
        return eventType;
    }

    // ========================================================================
    // Additional Event Sourcing Methods
    // ========================================================================

    /**
     * Gets unique event identifier for deduplication.
     *
     * @return event ID
     */
    public String getId() {
        return eventId;
    }

    /**
     * Gets timestamp when event occurred (UTC).
     *
     * @return occurred timestamp
     */
    public Instant getTimestamp() {
        return occurredAt;
    }

    /**
     * Gets causation ID for event causality tracking.
     *
     * @return causation ID
     */
    public String getCausationId() {
        return causationId;
    }

    /**
     * Checks if this event can be safely replayed.
     *
     * <p>Role update events are idempotent and safe to replay.
     *
     * @return true (always replayable)
     */
    public boolean isReplayable() {
        return true;
    }

    // ========================================================================
    // Business Methods — Role Transition Analysis
    // ========================================================================

    /**
     * Checks if this is a valid role transition according to the business rules map.
     *
     * @return true if transition is allowed by the system
     */
    public boolean isValidTransition() {
        if (oldRole == newRole) {
            return false;
        }
        Set<UserRole> validTargets = VALID_TRANSITIONS.get(oldRole);
        return validTargets != null && validTargets.contains(newRole);
    }

    /**
     * Checks if the new role represents an elevation in privilege/capability relative to the old
     * role.
     *
     * @return true if the user is gaining new capabilities
     */
    public boolean isElevatingRole() {
        return oldRole == UserRole.CUSTOMER
                && (newRole == UserRole.SELLER || newRole == UserRole.DELIVERY_AGENT);
    }

    /**
     * Checks if the new role represents a revocation of privilege/capability.
     *
     * @return true if the user is losing capabilities
     */
    public boolean isRevokingRole() {
        return (oldRole == UserRole.SELLER || oldRole == UserRole.DELIVERY_AGENT)
                && newRole == UserRole.CUSTOMER;
    }

    /**
     * Checks if the seller role was added by this transition.
     *
     * @return true if new role is SELLER and old role was not SELLER
     */
    public boolean isSellerRoleAdded() {
        return newRole == UserRole.SELLER && oldRole != UserRole.SELLER;
    }

    /**
     * Checks if the seller role was removed by this transition.
     *
     * @return true if old role was SELLER and new role is not SELLER
     */
    public boolean isSellerRoleRemoved() {
        return oldRole == UserRole.SELLER && newRole != UserRole.SELLER;
    }

    /**
     * Checks if the delivery agent role was added by this transition.
     *
     * @return true if new role is DELIVERY_AGENT and old role was not
     */
    public boolean isDeliveryAgentRoleAdded() {
        return newRole == UserRole.DELIVERY_AGENT && oldRole != UserRole.DELIVERY_AGENT;
    }

    /**
     * Checks if the delivery agent role was removed by this transition.
     *
     * @return true if old role was DELIVERY_AGENT and new role is not
     */
    public boolean isDeliveryAgentRoleRemoved() {
        return oldRole == UserRole.DELIVERY_AGENT && newRole != UserRole.DELIVERY_AGENT;
    }

    /**
     * Checks if this role change was approved by an administrator.
     *
     * @return true if change source is admin approval
     */
    public boolean isAdminApproved() {
        return changeSource == RoleChangeSource.ADMIN_APPROVAL;
    }

    /**
     * Checks if this role change was automatic/system-driven.
     *
     * @return true if change source is automatic promotion
     */
    public boolean isAutomatic() {
        return changeSource == RoleChangeSource.AUTOMATIC_PROMOTION;
    }

    /**
     * Checks if this event is for a specific user.
     *
     * @param userId user ID to check
     * @return true if event is for this user
     */
    public boolean isForUser(Long userId) {
        return Objects.equals(this.userId, userId);
    }

    /**
     * Checks if this event is of a specific type.
     *
     * @param eventType event type to check
     * @return true if this event matches the type
     */
    public boolean isType(String eventType) {
        return this.eventType.equals(eventType);
    }

    /**
     * Checks if event is recent (within specified seconds).
     *
     * @param seconds time window in seconds
     * @return true if event occurred within specified time
     */
    public boolean isRecent(long seconds) {
        if (seconds <= 0) {
            throw new DomainValidationException("seconds", "Recency window must be positive");
        }
        return Instant.now().minusSeconds(seconds).isBefore(occurredAt);
    }

    // ========================================================================
    // equals, hashCode & toString
    // ========================================================================

    /**
     * Two events are equal if and only if they share the same {@code eventId}. Used for
     * deduplication in Sets/Maps and idempotent event-store processing.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserRoleUpdatedEvent)) return false;
        UserRoleUpdatedEvent that = (UserRoleUpdatedEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    /** Audit-safe string representation. Excludes {@code userId} to prevent leaking PII in logs. */
    @Override
    public String toString() {
        return "UserRoleUpdatedEvent{"
                + "eventId='"
                + eventId
                + '\''
                + ", oldRole="
                + oldRole
                + ", newRole="
                + newRole
                + ", changeSource="
                + changeSource
                + ", occurredAt="
                + occurredAt
                + ", correlationId='"
                + correlationId
                + '\''
                + '}';
    }

    // ========================================================================
    // Nested Enum: Role Change Source
    // ========================================================================

    /**
     * Enumeration of sources/triggers for role changes.
     *
     * <p>Tracks how and why the role change was initiated, forming part of the audit trail.
     */
    public enum RoleChangeSource {

        /** Administrator explicitly approved the role change via admin panel. */
        ADMIN_APPROVAL("Admin Approval", "Administrator manually approved role change"),

        /** User self-requested the role change (may still need approval). */
        USER_REQUEST("User Request", "User requested to change their role"),

        /** System automatically promoted user based on activity or criteria. */
        AUTOMATIC_PROMOTION(
                "Automatic Promotion", "System automatically promoted user based on criteria"),

        /** Role changed due to policy enforcement or compliance requirement. */
        POLICY_ENFORCEMENT(
                "Policy Enforcement", "Role changed due to policy enforcement or compliance"),

        /** Role restricted due to suspension or violations. */
        SUSPENSION_ENFORCEMENT(
                "Suspension Enforcement", "Role restricted due to suspension or violations"),

        /** Internal system transition (e.g., aggregate domain behavior). */
        SYSTEM_INTERNAL("System Internal", "Role updated via internal aggregate domain behavior");

        /** Human-readable label for UI display. */
        private final String label;

        /** Detailed description for audit logs. */
        private final String description;

        RoleChangeSource(String label, String description) {
            this.label = label;
            this.description = description;
        }

        @JsonIgnore
        public String getLabel() {
            return label;
        }

        @JsonIgnore
        public String getDescription() {
            return description;
        }
    }
}
