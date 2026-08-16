package com.eshop.app.user.domain.event;

import com.eshop.app.core.events.contract.DomainEvent;
import com.eshop.app.core.exception.business.DomainValidationException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;

/**
 * Domain event published when a seller profile application is rejected.
 *
 * <p><b>Business Meaning:</b> A seller application has been reviewed and found to be non-compliant
 * with marketplace requirements. The seller is notified with a specific reason and given guidance
 * on how to address the issues for reapplication.
 *
 * <p><b>Event Guarantees:</b>
 *
 * <ul>
 *   <li>Published ONLY after explicit rejection decision (manual review or automated)
 *   <li>Seller profile exists in PENDING/UNDER_REVIEW state before rejection
 *   <li>Rejection reason is mandatory and descriptive (not empty)
 *   <li>Rejecting authority is identified and authorized
 *   <li>Rejection category is specified (document, compliance, quality, fraud)
 *   <li>Event is immutable after creation
 *   <li>Timestamp is UTC (timezone-independent)
 *   <li>Event ID ensures deduplication in event store
 * </ul>
 *
 * <p><b>State Transition:</b>
 *
 * <pre>
 * SellerProfile.Status:
 *
 * PENDING ──────────┬──> APPROVED (SellerApprovedEvent)
 *                   │
 *                   └──> REJECTED (THIS EVENT) ← Seller cannot sell
 *                             │
 *                             ├──> [Seller addresses issues]
 *                             │
 *                             └──> PENDING (SellerReappliedEvent)
 *                                       │
 *                                       └──> Review cycle repeats
 * </pre>
 *
 * <p><b>Seller Application Review Flow:</b>
 *
 * <pre>
 * SellerProfileCreatedEvent (1. Application submitted)
 *   ↓
 * SellerProfileUnderReviewEvent (2. Review started)
 *   ↓
 * [Reviewer checks documents, compliance, business details]
 *   ↓
 * [Decision: REJECT]
 *   ↓
 * SellerRejectedEvent (3. THIS EVENT - Rejection decision)
 *   ↓
 * [Concurrent Processing]
 *   ├─ NotificationService: Send rejection email with reason + guidance
 *   ├─ SellerProfileService: Update status to REJECTED
 *   ├─ AppealService: Open appeal window if applicable
 *   ├─ AuditService: Log rejection for compliance
 *   ├─ AnalyticsService: Track rejection metrics
 *   └─ QualityService: Update review quality metrics
 *   ↓
 * [Optional: SellerAppealSubmittedEvent if seller appeals]
 * [Optional: SellerReappliedEvent after resolving issues]
 * </pre>
 *
 * <p><b>Downstream Consumers & Responsibilities:</b>
 *
 * <ul>
 *   <li><b>NotificationService:</b> Send detailed rejection email with: specific reason, required
 *       actions, reapplication guidance
 *   <li><b>SellerProfileService:</b> Update profile status to REJECTED
 *   <li><b>AppealService:</b> Open appeal window if rejection allows appeal
 *   <li><b>AuditService:</b> Log for compliance documentation
 *   <li><b>AnalyticsService:</b> Track rejection reasons and rates
 *   <li><b>QualityService:</b> Monitor review quality and consistency
 *   <li><b>ReapplicationService:</b> Schedule reapplication eligibility
 * </ul>
 *
 * <p><b>Usage Examples:</b>
 *
 * <pre>{@code
 * // Creating event - Manual rejection by admin
 * SellerRejectedEvent event = SellerRejectedEvent.create(
 *     sellerProfileId,                              // Seller profile ID
 *     userId,                                       // User ID
 *     "Uploaded business license is expired",       // Specific reason
 *     List.of("business_license", "tax_id"),        // Failed documents
 *     RejectionCategory.INVALID_DOCUMENTS,          // Category
 *     RejectorType.ADMIN_REVIEWER,                  // Who rejected
 *     456L,                                         // Admin user ID
 *     "admin@eshop.com",                            // Admin identifier
 *     true,                                         // Can appeal?
 *     true,                                         // Can reapply?
 *     Duration.ofDays(30),                          // Wait before reapply
 *     "req-123",                                    // Correlation ID
 *     "cause-456"                                   // Causation ID
 * );
 *
 * // Consuming event
 * @EventListener
 * public void onSellerRejected(SellerRejectedEvent event) {
 *     if (event.canAppeal()) {
 *         appealService.openAppealWindow(event.getSellerProfileId());
 *     }
 *     if (event.isDocumentIssue()) {
 *         notificationService.sendDocumentRejectionEmail(event);
 *     } else if (event.isFraudDetected()) {
 *         securityService.flagAccount(event.getUserId());
 *     }
 * }
 * }</pre>
 *
 * @author Your Team
 * @version 1.0
 * @since 1.0
 * @see DomainEvent
 * @see SellerActivatedEvent
 */
@Getter
public final class SellerRejectedEvent implements DomainEvent {

    // ========================================================================
    // Constants
    // ========================================================================

    /** Event type identifier for this domain event. */
    public static final String EVENT_TYPE = "seller.rejected";

    /** Current event schema version. */
    public static final int EVENT_VERSION = 1;

    /** Maximum allowed length for rejection reason. */
    private static final int MAX_REASON_LENGTH = 1000;

    // ========================================================================
    // Event Identity & Metadata
    // ========================================================================

    /** Unique identifier for this event. Used for deduplication and idempotent processing. */
    @JsonProperty("eventId")
    private final String eventId;

    /** Type identifier for this event. */
    @JsonProperty("eventType")
    private final String eventType;

    /** Schema version of this event. */
    @JsonProperty("eventVersion")
    private final int eventVersion;

    /** UTC timestamp when rejection occurred. */
    @JsonProperty("occurredAt")
    private final Instant occurredAt;

    // ========================================================================
    // Distributed Tracing & Correlation
    // ========================================================================

    /** Correlation ID for distributed tracing. Links all events in rejection workflow. */
    @JsonProperty("correlationId")
    private final String correlationId;

    /** Causation ID for event causality. Identifies what triggered this rejection. */
    @JsonProperty("causationId")
    private final String causationId;

    // ========================================================================
    // Event Data - Seller Identity
    // ========================================================================

    /** Seller profile ID being rejected. Foreign key to {@code SellerProfile} aggregate. */
    @JsonProperty("sellerProfileId")
    private final Long sellerProfileId;

    /**
     * User ID associated with the rejected seller profile. Foreign key to {@code User} aggregate.
     */
    @JsonProperty("userId")
    private final Long userId;

    // ========================================================================
    // Event Data - Rejection Details
    // ========================================================================

    /**
     * Human-readable rejection reason. Must be descriptive enough for seller to understand and act
     * on.
     */
    @JsonProperty("rejectionReason")
    private final String rejectionReason;

    /** Category of rejection. Enables categorized notification templates and analytics. */
    @JsonProperty("rejectionCategory")
    private final RejectionCategory rejectionCategory;

    /**
     * List of specific document/field names that caused rejection. Helps seller understand exactly
     * what needs to be fixed.
     */
    @JsonProperty("failedFields")
    private final List<String> failedFields;

    /**
     * Optional additional note from reviewer. Provides extra context beyond the standard rejection
     * reason.
     */
    @lombok.Getter(lombok.AccessLevel.NONE)
    @JsonProperty("reviewerNote")
    private final String reviewerNote;

    // ========================================================================
    // Event Data - Rejector Information
    // ========================================================================

    /**
     * Type of entity that performed rejection. Determines notification format and appeal
     * eligibility.
     */
    @JsonProperty("rejectorType")
    private final RejectorType rejectorType;

    /** ID of the admin/system that performed rejection. For accountability and audit trail. */
    @JsonProperty("rejectorId")
    private final Long rejectorId;

    /** Identifier of the rejector (email, username). */
    @JsonProperty("rejectorIdentifier")
    private final String rejectorIdentifier;

    // ========================================================================
    // Event Data - Reapplication & Appeal Rights
    // ========================================================================

    /**
     * Indicates if seller can appeal this rejection. Automated/fraud rejections may not allow
     * appeals.
     */
    @JsonProperty("canAppeal")
    private final boolean canAppeal;

    /**
     * Indicates if seller can reapply after addressing issues. Fraud/blacklist rejections may not
     * allow reapplication.
     */
    @JsonProperty("canReapply")
    private final boolean canReapply;

    /** How long seller must wait before reapplying. Null if they can reapply immediately. */
    @JsonIgnore private final Duration reapplicationCooldown;

    // ========================================================================
    // Constructor & Factory Methods
    // ========================================================================

    /**
     * Primary factory method to create a fully validated SellerRejectedEvent.
     *
     * @param sellerProfileId seller profile ID
     * @param userId user ID
     * @param rejectionReason specific rejection reason
     * @param failedFields list of failed document/field names
     * @param rejectionCategory rejection category
     * @param rejectorType type of entity rejecting
     * @param rejectorId ID of rejecting entity
     * @param rejectorIdentifier identifier of rejector (email/username)
     * @param canAppeal whether seller can appeal
     * @param canReapply whether seller can reapply
     * @param reapplicationCooldown wait duration before reapplication
     * @param correlationId correlation ID for tracing
     * @param causationId causation ID
     * @return validated SellerRejectedEvent instance
     * @throws DomainValidationException if validation fails
     */
    public static SellerRejectedEvent create(
            Long sellerProfileId,
            Long userId,
            String rejectionReason,
            List<String> failedFields,
            RejectionCategory rejectionCategory,
            RejectorType rejectorType,
            Long rejectorId,
            String rejectorIdentifier,
            boolean canAppeal,
            boolean canReapply,
            Duration reapplicationCooldown,
            String correlationId,
            String causationId) {

        return SellerRejectedEvent.builder()
                .sellerProfileId(sellerProfileId)
                .userId(userId)
                .rejectionReason(rejectionReason)
                .failedFields(failedFields)
                .rejectionCategory(rejectionCategory)
                .rejectorType(rejectorType)
                .rejectorId(rejectorId)
                .rejectorIdentifier(rejectorIdentifier)
                .canAppeal(canAppeal)
                .canReapply(canReapply)
                .reapplicationCooldown(reapplicationCooldown)
                .correlationId(correlationId)
                .causationId(causationId)
                .build();
    }

    /**
     * Factory method for document-related rejection.
     *
     * @param sellerProfileId seller profile ID
     * @param userId user ID
     * @param reason rejection reason
     * @param failedDocuments list of invalid/missing documents
     * @param rejectorId admin ID
     * @param rejectorIdentifier admin email
     * @return validated event
     */
    public static SellerRejectedEvent createDocumentRejection(
            Long sellerProfileId,
            Long userId,
            String reason,
            List<String> failedDocuments,
            Long rejectorId,
            String rejectorIdentifier) {

        return create(
                sellerProfileId,
                userId,
                reason,
                failedDocuments,
                RejectionCategory.INVALID_DOCUMENTS,
                RejectorType.ADMIN_REVIEWER,
                rejectorId,
                rejectorIdentifier,
                true, // Can appeal
                true, // Can reapply
                Duration.ofDays(7), // 7-day cooldown to fix docs
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString());
    }

    /**
     * Factory method for fraud-related rejection.
     *
     * @param sellerProfileId seller profile ID
     * @param userId user ID
     * @param reason specific fraud reason
     * @param rejectorId system/admin ID
     * @param rejectorIdentifier system/admin identifier
     * @return validated event (no appeal, no reapply for fraud)
     */
    public static SellerRejectedEvent createFraudRejection(
            Long sellerProfileId,
            Long userId,
            String reason,
            Long rejectorId,
            String rejectorIdentifier) {

        return create(
                sellerProfileId,
                userId,
                reason,
                Collections.emptyList(),
                RejectionCategory.FRAUD_DETECTED,
                RejectorType.AUTOMATED_SYSTEM,
                rejectorId,
                rejectorIdentifier,
                false, // Cannot appeal
                false, // Cannot reapply
                null, // No reapplication
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString());
    }

    /**
     * Factory method for compliance-related rejection.
     *
     * @param sellerProfileId seller profile ID
     * @param userId user ID
     * @param reason rejection reason
     * @param failedCompliances failed compliance areas
     * @param rejectorId admin ID
     * @param rejectorIdentifier admin email
     * @return validated event
     */
    public static SellerRejectedEvent createComplianceRejection(
            Long sellerProfileId,
            Long userId,
            String reason,
            List<String> failedCompliances,
            Long rejectorId,
            String rejectorIdentifier) {

        return create(
                sellerProfileId,
                userId,
                reason,
                failedCompliances,
                RejectionCategory.COMPLIANCE_VIOLATION,
                RejectorType.ADMIN_REVIEWER,
                rejectorId,
                rejectorIdentifier,
                true, // Can appeal
                true, // Can reapply
                Duration.ofDays(30), // 30-day cooldown
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString());
    }

    /** Private constructor (use factory methods or builder instead). */
    @JsonCreator
    private SellerRejectedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("eventVersion") int eventVersion,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("causationId") String causationId,
            @JsonProperty("sellerProfileId") Long sellerProfileId,
            @JsonProperty("userId") Long userId,
            @JsonProperty("rejectionReason") String rejectionReason,
            @JsonProperty("rejectionCategory") RejectionCategory rejectionCategory,
            @JsonProperty("failedFields") List<String> failedFields,
            @JsonProperty("reviewerNote") String reviewerNote,
            @JsonProperty("rejectorType") RejectorType rejectorType,
            @JsonProperty("rejectorId") Long rejectorId,
            @JsonProperty("rejectorIdentifier") String rejectorIdentifier,
            @JsonProperty("canAppeal") boolean canAppeal,
            @JsonProperty("canReapply") boolean canReapply,
            @JsonProperty("reapplicationCooldownSeconds") Long reapplicationCooldownSeconds) {

        this.eventId = eventId != null ? eventId : UUID.randomUUID().toString();
        this.eventType = eventType != null ? eventType : EVENT_TYPE;
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
        this.sellerProfileId =
                Validator.requirePositive(
                        sellerProfileId, "sellerProfileId", "Seller profile ID must be positive");
        this.userId = Validator.requirePositive(userId, "userId", "User ID must be positive");
        this.rejectionReason = Validator.validateRejectionReason(rejectionReason);
        this.rejectionCategory =
                Validator.requireNonNull(
                        rejectionCategory,
                        "rejectionCategory",
                        "Rejection category is required for analytics and notification templates");
        this.failedFields = Validator.validateFailedFields(failedFields);
        this.reviewerNote = reviewerNote;
        this.rejectorType =
                Validator.requireNonNull(
                        rejectorType,
                        "rejectorType",
                        "Rejector type is required for accountability");
        this.rejectorId =
                Validator.requirePositive(
                        rejectorId,
                        "rejectorId",
                        "Rejector ID is required for audit accountability");
        this.rejectorIdentifier =
                Validator.requireNonBlank(
                        rejectorIdentifier,
                        "rejectorIdentifier",
                        "Rejector identifier is required for audit trail");
        this.canAppeal = canAppeal;
        this.canReapply = canReapply;

        Duration cooldown =
                reapplicationCooldownSeconds != null
                        ? Duration.ofSeconds(reapplicationCooldownSeconds)
                        : null;
        this.reapplicationCooldown = Validator.validateCooldown(cooldown);
    }

    // ========================================================================
    // Builder
    // ========================================================================

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String eventId = UUID.randomUUID().toString();
        private String eventType = EVENT_TYPE;
        private int eventVersion = EVENT_VERSION;
        private Instant occurredAt = Instant.now();
        private String correlationId = UUID.randomUUID().toString();
        private String causationId = UUID.randomUUID().toString();
        private Long sellerProfileId;
        private Long userId;
        private String rejectionReason;
        private RejectionCategory rejectionCategory;
        private List<String> failedFields = Collections.emptyList();
        private String reviewerNote;
        private RejectorType rejectorType;
        private Long rejectorId;
        private String rejectorIdentifier;
        private boolean canAppeal = true;
        private boolean canReapply = true;
        private Duration reapplicationCooldown;

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

        public Builder sellerProfileId(Long val) {
            this.sellerProfileId = val;
            return this;
        }

        public Builder userId(Long val) {
            this.userId = val;
            return this;
        }

        public Builder rejectionReason(String val) {
            this.rejectionReason = val;
            return this;
        }

        public Builder rejectionCategory(RejectionCategory val) {
            this.rejectionCategory = val;
            return this;
        }

        public Builder failedFields(List<String> val) {
            this.failedFields = val;
            return this;
        }

        public Builder reviewerNote(String val) {
            this.reviewerNote = val;
            return this;
        }

        public Builder rejectorType(RejectorType val) {
            this.rejectorType = val;
            return this;
        }

        public Builder rejectorId(Long val) {
            this.rejectorId = val;
            return this;
        }

        public Builder rejectorIdentifier(String val) {
            this.rejectorIdentifier = val;
            return this;
        }

        public Builder canAppeal(boolean val) {
            this.canAppeal = val;
            return this;
        }

        public Builder canReapply(boolean val) {
            this.canReapply = val;
            return this;
        }

        public Builder reapplicationCooldown(Duration val) {
            this.reapplicationCooldown = val;
            return this;
        }

        public SellerRejectedEvent build() {
            Long effectiveCooldownSeconds =
                    (this.reapplicationCooldown != null)
                            ? this.reapplicationCooldown.getSeconds()
                            : null;

            return new SellerRejectedEvent(
                    eventId,
                    eventType,
                    eventVersion,
                    occurredAt,
                    correlationId,
                    causationId,
                    sellerProfileId,
                    userId,
                    rejectionReason,
                    rejectionCategory,
                    failedFields,
                    reviewerNote,
                    rejectorType,
                    rejectorId,
                    rejectorIdentifier,
                    canAppeal,
                    canReapply,
                    effectiveCooldownSeconds);
        }
    }

    // =========================================================================
    // Inner Validator (DRY - Single place for all validation logic)
    // =========================================================================

    /** Internal validation utility. Centralizes all field validation to eliminate duplication. */
    private static final class Validator {

        private Validator() {} // Utility class

        static String requireNonBlank(String value, String field, String message) {
            return EventValidator.requireNonBlank(value, field, message);
        }

        static Long requirePositive(Long value, String field, String message) {
            return EventValidator.requirePositiveLong(value, field, message);
        }

        static <T> T requireNonNull(T value, String field, String message) {
            return EventValidator.requireNonNull(value, field, message);
        }

        static String validateRejectionReason(String reason) {
            requireNonBlank(
                    reason,
                    "rejectionReason",
                    "Rejection reason is required - seller needs to know why and what to fix");

            String normalized = reason.trim();

            if (normalized.length() < 10) {
                throw new DomainValidationException(
                        "rejectionReason",
                        "Rejection reason is too short (min 10 chars) - must be descriptive");
            }

            if (normalized.length() > MAX_REASON_LENGTH) {
                throw new DomainValidationException(
                        "rejectionReason",
                        String.format(
                                "Rejection reason is too long (max %d chars)", MAX_REASON_LENGTH));
            }

            return normalized;
        }

        static List<String> validateFailedFields(List<String> fields) {
            if (fields == null) {
                return Collections.emptyList();
            }

            // Validate no null/blank entries
            boolean hasInvalid =
                    fields.stream().anyMatch(field -> field == null || field.isBlank());

            if (hasInvalid) {
                throw new DomainValidationException(
                        "failedFields", "Failed fields list cannot contain null or blank entries");
            }

            return Collections.unmodifiableList(List.copyOf(fields));
        }

        static Duration validateCooldown(Duration cooldown) {
            if (cooldown == null) {
                return null; // Optional field
            }
            if (cooldown.isNegative()) {
                throw new DomainValidationException(
                        "reapplicationCooldown", "Reapplication cooldown cannot be negative");
            }
            return cooldown;
        }
    }

    // ========================================================================
    // DomainEvent Implementation
    // ========================================================================

    /** {@inheritDoc} */
    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    /** {@inheritDoc} */
    @Override
    public long getOccurredAt() {
        return occurredAt.toEpochMilli();
    }

    /** {@inheritDoc} */
    @Override
    public String getEventType() {
        return eventType;
    }

    // ========================================================================
    // DomainEvent Helpers (Matches TwoFactorEnabledEvent standard)
    // ========================================================================

    public String getId() {
        return eventId;
    }

    public String getType() {
        return eventType;
    }

    public Instant getTimestamp() {
        return occurredAt;
    }

    public String getCausationId() {
        return causationId;
    }

    public boolean isReplayable() {
        return true;
    }

    // ========================================================================
    // Business Methods - Rejection Analysis
    // ========================================================================

    /**
     * Checks if rejection is document-related.
     *
     * @return true if documents caused rejection
     */
    public boolean isDocumentIssue() {
        return rejectionCategory == RejectionCategory.INVALID_DOCUMENTS
                || rejectionCategory == RejectionCategory.MISSING_DOCUMENTS;
    }

    /**
     * Checks if rejection is fraud-related. Triggers additional security measures downstream.
     *
     * @return true if fraud detected
     */
    public boolean isFraudDetected() {
        return rejectionCategory == RejectionCategory.FRAUD_DETECTED;
    }

    /**
     * Checks if rejection is compliance-related.
     *
     * @return true if compliance violation
     */
    public boolean isComplianceViolation() {
        return rejectionCategory == RejectionCategory.COMPLIANCE_VIOLATION;
    }

    /**
     * Checks if rejection is quality-related.
     *
     * @return true if quality standards not met
     */
    public boolean isQualityIssue() {
        return rejectionCategory == RejectionCategory.QUALITY_STANDARDS;
    }

    /**
     * Checks if rejection was by a human reviewer.
     *
     * @return true if rejected by human
     */
    public boolean isHumanRejection() {
        return rejectorType == RejectorType.ADMIN_REVIEWER
                || rejectorType == RejectorType.SENIOR_REVIEWER
                || rejectorType == RejectorType.COMPLIANCE_OFFICER;
    }

    /**
     * Checks if rejection was automated.
     *
     * @return true if rejected by automated system
     */
    public boolean isAutomatedRejection() {
        return rejectorType == RejectorType.AUTOMATED_SYSTEM;
    }

    /**
     * Checks if seller has specific failed fields to address.
     *
     * @return true if failed fields are specified
     */
    public boolean hasFailedFields() {
        return !failedFields.isEmpty();
    }

    /**
     * Checks if rejection has a reviewer note.
     *
     * @return true if reviewer note is present
     */
    public boolean hasReviewerNote() {
        return reviewerNote != null && !reviewerNote.isBlank();
    }

    /**
     * Checks if there is a reapplication cooldown period.
     *
     * @return true if seller must wait before reapplying
     */
    public boolean hasReapplicationCooldown() {
        return reapplicationCooldown != null && !reapplicationCooldown.isZero();
    }

    /**
     * Checks if this rejection is permanent (cannot appeal or reapply).
     *
     * @return true if this is a permanent rejection
     */
    public boolean isPermanentRejection() {
        return !canAppeal && !canReapply;
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

    /**
     * Gets reviewer note.
     *
     * @return reviewer note
     */
    public String getReviewerNote() {
        return reviewerNote;
    }

    /**
     * Gets reviewer note as Optional.
     *
     * @return Optional containing reviewer note
     */
    public Optional<String> getReviewerNoteOptional() {
        return Optional.ofNullable(reviewerNote);
    }

    /**
     * Gets reapplication cooldown duration in seconds.
     *
     * @return cooldown in seconds, or null if none
     */
    @JsonProperty("reapplicationCooldownSeconds")
    public Long getReapplicationCooldownSeconds() {
        return reapplicationCooldown != null ? reapplicationCooldown.getSeconds() : null;
    }

    /**
     * Gets reapplication cooldown as Optional.
     *
     * @return Optional containing cooldown duration
     */
    public Optional<Duration> getReapplicationCooldownOptional() {
        return Optional.ofNullable(reapplicationCooldown);
    }

    /**
     * Gets earliest reapplication date.
     *
     * @return Instant when seller can reapply, or now if no cooldown
     */
    public Instant getEarliestReapplicationDate() {
        if (reapplicationCooldown == null || reapplicationCooldown.isZero()) {
            return occurredAt;
        }
        return occurredAt.plus(reapplicationCooldown);
    }

    /**
     * Gets formatted cooldown duration.
     *
     * @return formatted cooldown string
     */
    public String getFormattedCooldown() {
        if (reapplicationCooldown == null) {
            return "No cooldown";
        }

        long days = reapplicationCooldown.toDays();
        long hours = reapplicationCooldown.toHours() % 24;

        if (days > 0) return String.format("%d days, %d hours", days, hours);
        if (hours > 0) return String.format("%d hours", hours);
        return "Less than 1 hour";
    }

    /**
     * Gets count of failed fields.
     *
     * @return number of failed fields
     */
    public int getFailedFieldCount() {
        return failedFields.size();
    }

    // ========================================================================
    // equals & hashCode
    // ========================================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SellerRejectedEvent)) return false;
        SellerRejectedEvent that = (SellerRejectedEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    // ========================================================================
    // String Representation
    // ========================================================================

    /**
     * Returns detailed string for logging. Excludes rejector identifier (privacy).
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return "SellerRejectedEvent{"
                + "eventId='"
                + eventId
                + '\''
                + ", eventType='"
                + eventType
                + '\''
                + ", sellerProfileId="
                + sellerProfileId
                + ", userId="
                + userId
                + ", rejectionCategory="
                + rejectionCategory
                + ", rejectorType="
                + rejectorType
                + ", rejectorId="
                + rejectorId
                + ", failedFieldCount="
                + getFailedFieldCount()
                + ", canAppeal="
                + canAppeal
                + ", canReapply="
                + canReapply
                + ", isPermanent="
                + isPermanentRejection()
                + ", occurredAt="
                + occurredAt
                + ", correlationId='"
                + correlationId
                + '\''
                + '}';
    }

    // ========================================================================
    // Nested Enums
    // ========================================================================

    /**
     * Enumeration of rejection categories. Enables categorized notifications, templates, and
     * analytics.
     */
    public enum RejectionCategory {

        /** Submitted documents are invalid (expired, forged, wrong type). */
        INVALID_DOCUMENTS(
                "Invalid Documents",
                "Submitted documents are invalid or not accepted",
                true, // Can appeal
                true // Can reapply
                ),

        /** Required documents are missing. */
        MISSING_DOCUMENTS(
                "Missing Documents",
                "Required documents were not submitted",
                false, // No need to appeal - just resubmit
                true // Can reapply
                ),

        /** Business information is incorrect or inconsistent. */
        INCORRECT_BUSINESS_INFO(
                "Incorrect Business Info",
                "Business information is incorrect or inconsistent with documents",
                true,
                true),

        /** Seller violated compliance regulations. */
        COMPLIANCE_VIOLATION(
                "Compliance Violation",
                "Application violates platform compliance regulations",
                true,
                true),

        /** Quality standards not met (e.g., store quality, product standards). */
        QUALITY_STANDARDS(
                "Quality Standards",
                "Application does not meet platform quality standards",
                true,
                true),

        /**
         * Fraud detected - automatic or manual fraud identification. Permanent or long-term
         * rejection.
         */
        FRAUD_DETECTED(
                "Fraud Detected",
                "Fraudulent activity or documents detected",
                false, // Cannot appeal fraud rejection
                false // Cannot reapply
                ),

        /** Policy violation (e.g., duplicate account, blacklisted entity). */
        POLICY_VIOLATION(
                "Policy Violation",
                "Application violates marketplace policies",
                true,
                false // Typically cannot reapply immediately
                );

        private final String label;
        private final String description;
        private final boolean defaultCanAppeal;
        private final boolean defaultCanReapply;

        RejectionCategory(
                String label,
                String description,
                boolean defaultCanAppeal,
                boolean defaultCanReapply) {
            this.label = label;
            this.description = description;
            this.defaultCanAppeal = defaultCanAppeal;
            this.defaultCanReapply = defaultCanReapply;
        }

        @JsonIgnore
        public String getLabel() {
            return label;
        }

        @JsonIgnore
        public String getDescription() {
            return description;
        }

        @JsonIgnore
        public boolean isDefaultCanAppeal() {
            return defaultCanAppeal;
        }

        @JsonIgnore
        public boolean isDefaultCanReapply() {
            return defaultCanReapply;
        }
    }

    /** Enumeration of entity types that can reject seller applications. */
    public enum RejectorType {

        /** Standard admin reviewer. */
        ADMIN_REVIEWER("Admin Reviewer", "Standard admin reviewer - appeals allowed", true),

        /** Senior reviewer (higher authority, typically final decision). */
        SENIOR_REVIEWER(
                "Senior Reviewer",
                "Senior reviewer with final decision authority",
                false // Senior decision typically final
                ),

        /** Compliance officer (legal/regulatory rejection). */
        COMPLIANCE_OFFICER(
                "Compliance Officer", "Compliance officer - regulatory or legal rejection", true),

        /** Automated system (fraud detection, duplicate detection, etc.). */
        AUTOMATED_SYSTEM(
                "Automated System",
                "Automated rejection by system rules",
                true // Can appeal automated decisions to human reviewer
                );

        private final String label;
        private final String description;

        /** Whether this rejector type allows appeals by default. */
        private final boolean defaultAllowsAppeal;

        RejectorType(String label, String description, boolean defaultAllowsAppeal) {
            this.label = label;
            this.description = description;
            this.defaultAllowsAppeal = defaultAllowsAppeal;
        }

        @JsonIgnore
        public String getLabel() {
            return label;
        }

        @JsonIgnore
        public String getDescription() {
            return description;
        }

        @JsonIgnore
        public boolean isDefaultAllowsAppeal() {
            return defaultAllowsAppeal;
        }
    }
}
