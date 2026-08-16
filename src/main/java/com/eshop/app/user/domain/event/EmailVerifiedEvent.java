package com.eshop.app.user.domain.event;

import com.eshop.app.core.events.contract.DomainEvent;
import com.eshop.app.core.exception.business.DomainValidationException;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
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
 * Domain event published when a user's email address is successfully verified.
 *
 * <p><b>Business Meaning:</b> This event signals that a user has completed email verification,
 * enabling access to core platform features. The verification process ensures email ownership and
 * enables email-based communication.
 *
 * <p><b>Event Guarantees:</b>
 *
 * <ul>
 *   <li>Published ONLY after successful verification (idempotent)
 *   <li>Email address is valid and verified to belong to user
 *   <li>Event is immutable after creation
 *   <li>Event timestamp is absolute (UTC)
 *   <li>Event ID ensures deduplication in event store
 * </ul>
 *
 * <p><b>Event Sequence:</b>
 *
 * <pre>
 * EmailVerificationRequestedEvent (1. Verification initiated)
 *   ↓
 * EmailVerificationCodeSentEvent (2. Code sent to user's email)
 *   ↓
 * [User clicks link / enters code]
 *   ↓
 * EmailVerifiedEvent (3. Verification successful) ← THIS EVENT
 *   ↓
 * UserActivatedEvent (4. User account fully activated)
 * </pre>
 *
 * <p><b>Downstream Consumers:</b>
 *
 * <ul>
 *   <li><b>UserService:</b> Mark email as verified in user profile
 *   <li><b>NotificationService:</b> Send welcome email
 *   <li><b>AnalyticsService:</b> Track conversion metrics
 *   <li><b>MarketingService:</b> Add to email campaign list
 *   <li><b>SecurityService:</b> Update account security status
 * </ul>
 *
 * <p><b>Usage Examples:</b>
 *
 * <pre>{@code
 * // Creating event (in aggregate)
 * EmailVerifiedEvent event = EmailVerifiedEvent.create(
 *     userId,                          // User ID
 *     "user@example.com",              // Verified email
 *     "req-123",                       // Correlation ID
 *     "cause-456"                      // Causation ID
 * );
 *
 * // Publishing event
 * eventPublisher.publish(event);
 *
 * // Consuming event
 * @EventListener
 * public void onEmailVerified(EmailVerifiedEvent event) {
 *     userRepository.markEmailVerified(event.getUserId(), event.getEmail());
 *     notificationService.sendWelcomeEmail(event.getEmail());
 * }
 *
 * // Type checking
 * if (event.getEventType().equals(EmailVerifiedEvent.EVENT_TYPE)) {
 *     // Handle email verification event
 * }
 * }</pre>
 *
 * <h3>Event Sourcing Support:</h3>
 *
 * <p>This event is designed for event sourcing with support for:
 *
 * <ul>
 *   <li>Event replay for aggregate reconstruction
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
 * @see EmailVerificationRequestedEvent
 * @see EmailVerificationCodeSentEvent
 * @see UserActivatedEvent
 */
@Getter
@Builder(toBuilder = true)
@ToString(exclude = {"userId"}) // Don't log sensitive user ID
public final class EmailVerifiedEvent implements DomainEvent {

    // ========================================================================
    // Constants
    // ========================================================================

    /**
     * Event type identifier for this domain event. Used for polymorphic event handling and event
     * store querying.
     */
    public static final String EVENT_TYPE = "user.email.verified";

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
     * <p>Example: All events from single user action share correlation ID.
     */
    @NotBlank(message = "Correlation ID is required")
    private final String correlationId;

    /**
     * Causation ID for event causality tracking. Identifies the event that caused this event.
     *
     * <p>Example: EmailVerificationCodeSentEvent → EmailVerifiedEvent
     */
    @NotBlank(message = "Causation ID is required")
    private final String causationId;

    // ========================================================================
    // Event Data
    // ========================================================================

    /** User ID who verified their email. Foreign key to {@code User} aggregate. */
    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    private final Long userId;

    /**
     * Email address that was verified. Must be valid email format and verified to belong to user.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid format")
    private final String email;

    // ========================================================================
    // Constructor & Factory Methods
    // ========================================================================

    /**
     * Factory method to create a validated EmailVerifiedEvent.
     *
     * @param userId user ID (must be positive)
     * @param email verified email address (must be valid)
     * @param correlationId correlation ID for distributed tracing
     * @param causationId causation ID (event that caused this)
     * @return validated EmailVerifiedEvent instance
     * @throws DomainValidationException if validation fails
     */
    public static EmailVerifiedEvent create(
            Long userId, String email, String correlationId, String causationId) {

        return EmailVerifiedEvent.builder()
                .userId(validateUserId(userId))
                .email(validateEmail(email))
                .correlationId(validateCorrelationId(correlationId))
                .causationId(validateCausationId(causationId))
                .build();
    }

    /**
     * Factory method with minimal required parameters. Uses default values for
     * correlation/causation IDs.
     *
     * @param userId user ID
     * @param email verified email address
     * @return validated EmailVerifiedEvent instance
     */
    public static EmailVerifiedEvent createSimple(Long userId, String email) {
        return create(
                userId,
                email,
                UUID.randomUUID().toString(), // Auto-generate correlation ID
                UUID.randomUUID().toString() // Auto-generate causation ID
                );
    }

    /**
     * Legacy constructor matching the original record signature. Enables backward compatibility
     * with direct constructor calls in existing code while injecting reliable tracing defaults.
     *
     * @param userId user ID
     * @param email verified email address
     */
    public EmailVerifiedEvent(Long userId, String email) {
        this(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                EVENT_VERSION,
                Instant.now(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                userId,
                email);
    }

    /** Private constructor (use builder or factory methods instead). */
    private EmailVerifiedEvent(
            String eventId,
            String eventType,
            int eventVersion,
            Instant occurredAt,
            String correlationId,
            String causationId,
            Long userId,
            String email) {

        this.eventId =
                eventId == null || eventId.isBlank() ? UUID.randomUUID().toString() : eventId;
        this.eventType = eventType == null || eventType.isBlank() ? EVENT_TYPE : eventType;
        this.eventVersion = eventVersion <= 0 ? EVENT_VERSION : eventVersion;
        this.occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        this.correlationId = validateCorrelationId(correlationId);
        this.causationId = validateCausationId(causationId);
        this.userId = validateUserId(userId);
        this.email = validateEmail(email);
    }

    // ========================================================================
    // Validation Helpers (DRY Principle)
    // ========================================================================

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
     * Validates email address format and content.
     *
     * @param email email to validate
     * @return normalized email (trimmed, lowercase)
     * @throws DomainValidationException if invalid
     */
    private static String validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new DomainValidationException("email", "Email is required and cannot be blank");
        }

        String normalized = email.trim().toLowerCase();

        // Basic email validation (RFC 5322 simplified)
        if (!normalized.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new DomainValidationException(
                    "email", "Email format is invalid — must match RFC 5322 pattern");
        }

        if (normalized.length() > 254) {
            throw new DomainValidationException(
                    "email", "Email address is too long (max 254 characters)");
        }

        return normalized;
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
     * @return occurred timestamp
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
    // Additional DomainEvent / Event Sourcing Methods
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
     * <p>Email verification events are idempotent and safe to replay.
     *
     * @return true if event is replayable
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
     * Checks if this event is for a specific user.
     *
     * @param userId user ID to check
     * @return true if event is for this user
     */
    public boolean isForUser(Long userId) {
        return Objects.equals(this.userId, userId);
    }

    /**
     * Checks if this event is for a specific email.
     *
     * @param email email to check
     * @return true if event is for this email (case-insensitive)
     */
    public boolean isForEmail(String email) {
        if (email == null) {
            return false;
        }
        return this.email.equalsIgnoreCase(email.trim());
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
