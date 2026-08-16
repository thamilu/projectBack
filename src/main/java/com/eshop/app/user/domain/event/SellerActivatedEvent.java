package com.eshop.app.user.domain.event;

import com.eshop.app.core.events.contract.DomainEvent;
import com.eshop.app.core.exception.business.DomainValidationException;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Domain event published when a suspended/inactive seller profile is reactivated.
 *
 * <p><b>Business Meaning:</b> This event signals that a seller who was previously suspended or
 * inactive has been reactivated and can resume marketplace operations. This is a critical
 * operational event that affects seller visibility, store accessibility, and payment processing.
 *
 * <p><b>State Transitions:</b>
 *
 * <pre>
 * SUSPENDED ──(appeal approved)──&gt; SellerActivatedEvent → ACTIVE
 * INACTIVE  ──(reactivation)─────&gt; SellerActivatedEvent → ACTIVE
 * </pre>
 *
 * <p><b>Event Guarantees:</b>
 *
 * <ul>
 *   <li>Published ONLY after successful profile status transition
 *   <li>Seller profile exists and has valid approval
 *   <li>Associated user account is active and verified
 *   <li>Event is immutable after creation
 *   <li>Event timestamp is absolute (UTC)
 *   <li>Event ID ensures deduplication in event store
 * </ul>
 *
 * <p><b>Event Sequence:</b>
 *
 * <pre>
 * SellerApprovedEvent (1. Initial approval)
 *   ↓
 * [Seller violates policy or becomes inactive]
 *   ↓
 * SellerRejectedEvent / SellerSuspendedEvent (2. Suspension/Rejection)
 *   ↓
 * [Seller submits appeal / completes corrective action]
 *   ↓
 * SellerActivatedEvent (3. Reactivation approved) ← THIS EVENT
 * </pre>
 *
 * <p><b>Downstream Consumers:</b>
 *
 * <ul>
 *   <li><b>SellerService:</b> Update seller profile status to ACTIVE
 *   <li><b>StoreService:</b> Restore store visibility in marketplace
 *   <li><b>PaymentService:</b> Restore payout processing
 *   <li><b>NotificationService:</b> Send reactivation confirmation email
 *   <li><b>AnalyticsService:</b> Track seller retention metrics
 *   <li><b>AuditService:</b> Log operational change for compliance
 * </ul>
 *
 * <p><b>Usage Examples:</b>
 *
 * <pre>{@code
 * // Creating event (in aggregate)
 * SellerActivatedEvent event = SellerActivatedEvent.create(
 *     sellerProfileId,                 // Seller profile ID
 *     userId,                          // Associated user ID
 *     "req-123",                       // Correlation ID
 *     "cause-456"                      // Causation ID
 * );
 *
 * // Publishing event
 * eventPublisher.publish(event);
 *
 * // Consuming event
 * @EventListener
 * public void onSellerActivated(SellerActivatedEvent event) {
 *     sellerService.activateProfile(event.getSellerProfileId());
 *     storeService.restoreStoreVisibility(event.getSellerProfileId());
 *     notificationService.sendActivationEmail(event.getUserId());
 * }
 * }</pre>
 *
 * <h3>Event Sourcing Support:</h3>
 *
 * <p>This event is designed for event sourcing with support for:
 *
 * <ul>
 *   <li>Event replay for seller state reconstruction
 *   <li>Event versioning (current version: 1)
 *   <li>Distributed tracing (correlation/causation IDs)
 *   <li>Idempotent processing (event ID)
 *   <li>Time-based projections (occurred timestamp)
 * </ul>
 *
 * @author Your Team
 * @version 1.0
 * @since 1.0
 * @see DomainEvent
 * @see SellerApprovedEvent
 * @see SellerRejectedEvent
 */
@Getter
@Builder(toBuilder = true)
@ToString(exclude = {"userId"}) // Don't log sensitive user ID
public final class SellerActivatedEvent implements DomainEvent {

    // ========================================================================
    // Constants
    // ========================================================================

    /**
     * Event type identifier for this domain event. Used for polymorphic event handling and event
     * store querying.
     */
    public static final String EVENT_TYPE = "seller.activated";

    /**
     * Current event schema version. Incremented when event structure changes (for schema
     * evolution).
     */
    public static final int EVENT_VERSION = 1;

    // ========================================================================
    // Event Identity & Metadata
    // ========================================================================

    /**
     * Unique identifier for this event. Used for deduplication and idempotent processing.
     *
     * <p>Generated once at creation time, never changes.
     */
    @NotBlank(message = "Event ID is required")
    @Builder.Default
    private final String eventId = UUID.randomUUID().toString();

    /** Type identifier for this event. Enables polymorphic handling of different event types. */
    @NotBlank(message = "Event type is required")
    @Builder.Default
    @JsonProperty("type")
    private final String eventType = EVENT_TYPE;

    /** Schema version of this event. Enables handling of event structure evolution. */
    @Positive(message = "Event version must be positive")
    @Builder.Default
    private final int eventVersion = EVENT_VERSION;

    /**
     * Timestamp when event occurred (UTC). Absolute time for event ordering and time-based queries.
     */
    @NotNull(message = "Event occurred timestamp is required")
    @Builder.Default
    private final Instant occurredAt = Instant.now();

    // ========================================================================
    // Distributed Tracing & Correlation
    // ========================================================================

    /**
     * Correlation ID for distributed tracing. Links related events across multiple
     * services/transactions.
     *
     * <p>Example: All events from a seller appeal share the same correlation ID.
     */
    @NotBlank(message = "Correlation ID is required")
    private final String correlationId;

    /**
     * Causation ID for event causality tracking. Identifies the event that caused this event.
     *
     * <p>Example: SellerAppealSubmittedEvent → SellerActivatedEvent
     */
    @NotBlank(message = "Causation ID is required")
    private final String causationId;

    // ========================================================================
    // Event Data
    // ========================================================================

    /** Seller profile ID being reactivated. Foreign key to {@code SellerProfile} aggregate. */
    @NotNull(message = "Seller profile ID is required")
    @Positive(message = "Seller profile ID must be positive")
    private final Long sellerProfileId;

    /** User ID associated with the seller profile. Foreign key to {@code User} aggregate. */
    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    private final Long userId;

    // ========================================================================
    // Constructor & Factory Methods
    // ========================================================================

    /**
     * Factory method to create a validated SellerActivatedEvent.
     *
     * @param sellerProfileId seller profile ID (must be positive)
     * @param userId associated user ID (must be positive)
     * @param correlationId correlation ID for distributed tracing
     * @param causationId causation ID (event that caused this)
     * @return validated SellerActivatedEvent instance
     * @throws DomainValidationException if validation fails
     */
    public static SellerActivatedEvent create(
            Long sellerProfileId, Long userId, String correlationId, String causationId) {

        return SellerActivatedEvent.builder()
                .sellerProfileId(validateSellerProfileId(sellerProfileId))
                .userId(validateUserId(userId))
                .correlationId(validateCorrelationId(correlationId))
                .causationId(validateCausationId(causationId))
                .build();
    }

    /**
     * Factory method with minimal required parameters. Uses auto-generated correlation/causation
     * IDs.
     *
     * @param sellerProfileId seller profile ID
     * @param userId associated user ID
     * @return validated SellerActivatedEvent instance
     */
    public static SellerActivatedEvent createSimple(Long sellerProfileId, Long userId) {
        return create(
                sellerProfileId,
                userId,
                UUID.randomUUID().toString(), // Auto-generate correlation ID
                UUID.randomUUID().toString() // Auto-generate causation ID
                );
    }

    /**
     * Legacy constructor matching the original record signature. Enables backward compatibility
     * with direct constructor calls in existing aggregate code (e.g., {@code
     * SellerProfile.activateSeller()}).
     *
     * @param sellerProfileId seller profile ID
     * @param userId associated user ID
     */
    public SellerActivatedEvent(Long sellerProfileId, Long userId) {
        this(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                EVENT_VERSION,
                Instant.now(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                sellerProfileId,
                userId);
    }

    /** Private all-args constructor (use builder or factory methods instead). */
    private SellerActivatedEvent(
            String eventId,
            String eventType,
            int eventVersion,
            Instant occurredAt,
            String correlationId,
            String causationId,
            Long sellerProfileId,
            Long userId) {

        this.eventId =
                eventId == null || eventId.isBlank() ? UUID.randomUUID().toString() : eventId;
        this.eventType = eventType == null || eventType.isBlank() ? EVENT_TYPE : eventType;
        this.eventVersion = eventVersion <= 0 ? EVENT_VERSION : eventVersion;
        this.occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        this.correlationId = validateCorrelationId(correlationId);
        this.causationId = validateCausationId(causationId);
        this.sellerProfileId = validateSellerProfileId(sellerProfileId);
        this.userId = validateUserId(userId);
    }

    // ========================================================================
    // Validation Helpers (DRY Principle)
    // ========================================================================

    /**
     * Validates seller profile ID.
     *
     * @param sellerProfileId seller profile ID to validate
     * @return validated seller profile ID
     * @throws DomainValidationException if invalid
     */
    private static Long validateSellerProfileId(Long sellerProfileId) {
        return EventValidator.requirePositiveLong(
                sellerProfileId,
                "sellerProfileId",
                "Seller profile ID is required and must be positive");
    }

    /**
     * Validates user ID.
     *
     * @param userId user ID to validate
     * @return validated user ID
     * @throws DomainValidationException if invalid
     */
    private static Long validateUserId(Long userId) {
        return EventValidator.requirePositiveLong(
                userId, "userId", "User ID is required and must be positive");
    }

    /**
     * Validates correlation ID.
     *
     * @param correlationId correlation ID to validate
     * @return validated correlation ID
     * @throws DomainValidationException if invalid
     */
    private static String validateCorrelationId(String correlationId) {
        return EventValidator.requireNonBlank(
                correlationId,
                "correlationId",
                "Correlation ID is required for distributed tracing");
    }

    /**
     * Validates causation ID.
     *
     * @param causationId causation ID to validate
     * @return validated causation ID
     * @throws DomainValidationException if invalid
     */
    private static String validateCausationId(String causationId) {
        return EventValidator.requireNonBlank(
                causationId, "causationId", "Causation ID is required to track event causality");
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
     * <p>Seller activation events are idempotent and safe to replay.
     *
     * @return true (always replayable)
     */
    public boolean isReplayable() {
        return true;
    }

    // ========================================================================
    // Business Methods
    // ========================================================================

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
     * Checks if this event is for a specific seller.
     *
     * @param sellerProfileId seller profile ID to check
     * @return true if event is for this seller
     */
    public boolean isForSeller(Long sellerProfileId) {
        return Objects.equals(this.sellerProfileId, sellerProfileId);
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
}
