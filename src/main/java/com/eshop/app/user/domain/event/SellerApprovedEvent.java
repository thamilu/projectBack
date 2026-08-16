package com.eshop.app.user.domain.event;

import com.eshop.app.core.events.contract.DomainEvent;
import com.eshop.app.core.exception.business.DomainValidationException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;

/**
 * Domain event published when a seller profile application is approved.
 *
 * <p><b>Guarantees:</b>
 *
 * <ul>
 *   <li>Immutable after creation
 *   <li>All fields validated at construction time
 *   <li>UTC timestamps only (Instant, never LocalDateTime)
 *   <li>Idempotent (eventId prevents duplicate processing)
 *   <li>JSON serializable/deserializable (Kafka support)
 * </ul>
 *
 * @see DomainEvent
 * @see SellerRejectedEvent
 */
@Getter
public final class SellerApprovedEvent implements DomainEvent {

    // =========================================================================
    // Constants
    // =========================================================================

    public static final String EVENT_TYPE = "seller.approved";
    public static final int EVENT_VERSION = 1;

    private static final Duration STANDARD_SLA = Duration.ofHours(48);
    private static final Duration MINIMUM_REVIEW_DURATION = Duration.ofMinutes(1);

    // =========================================================================
    // Event Identity & Metadata
    // =========================================================================

    @JsonProperty("eventId")
    private final String eventId;

    @JsonProperty("eventType")
    private final String eventType;

    @JsonProperty("eventVersion")
    private final int eventVersion;

    @JsonProperty("occurredAt")
    private final Instant occurredAt;

    // =========================================================================
    // Distributed Tracing
    // =========================================================================

    @JsonProperty("correlationId")
    private final String correlationId;

    @JsonProperty("causationId")
    private final String causationId;

    // =========================================================================
    // Seller Identity
    // =========================================================================

    @JsonProperty("sellerProfileId")
    private final Long sellerProfileId;

    @JsonProperty("userId")
    private final Long userId;

    // =========================================================================
    // Approval Details
    // =========================================================================

    @JsonProperty("approvalTier")
    private final ApprovalTier approvalTier;

    @JsonProperty("approvalSource")
    private final ApprovalSource approvalSource;

    @JsonProperty("commissionRate")
    private final BigDecimal commissionRate;

    @JsonProperty("maxProductListings")
    private final int maxProductListings;

    @JsonIgnore private final Duration reviewDuration;

    @JsonProperty("verifiedDocuments")
    private final List<String> verifiedDocuments;

    @lombok.Getter(lombok.AccessLevel.NONE)
    @JsonProperty("approvalNote")
    private final String approvalNote;

    @lombok.Getter(lombok.AccessLevel.NONE)
    @JsonProperty("approvalConditions")
    private final String approvalConditions;

    // =========================================================================
    // Approver Information
    // =========================================================================

    @JsonProperty("approverId")
    private final Long approverId;

    @JsonProperty("approverIdentifier")
    private final String approverIdentifier;

    // =========================================================================
    // Constructor (Single source of truth)
    // =========================================================================

    /**
     * Single private constructor - ALL creation paths go through here. Validates ALL fields at
     * construction time. No object can exist in invalid state.
     */
    @JsonCreator
    private SellerApprovedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("eventVersion") int eventVersion,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("causationId") String causationId,
            @JsonProperty("sellerProfileId") Long sellerProfileId,
            @JsonProperty("userId") Long userId,
            @JsonProperty("approvalTier") ApprovalTier approvalTier,
            @JsonProperty("approvalSource") ApprovalSource approvalSource,
            @JsonProperty("commissionRate") BigDecimal commissionRate,
            @JsonProperty("maxProductListings") int maxProductListings,
            @JsonProperty("reviewDurationSeconds") long reviewDurationSeconds,
            @JsonProperty("verifiedDocuments") List<String> verifiedDocuments,
            @JsonProperty("approvalNote") String approvalNote,
            @JsonProperty("approvalConditions") String approvalConditions,
            @JsonProperty("approverId") Long approverId,
            @JsonProperty("approverIdentifier") String approverIdentifier) {

        // Identity (system-generated defaults for JSON deserialization)
        this.eventId = eventId != null ? eventId : UUID.randomUUID().toString();
        this.eventType = eventType != null ? eventType : EVENT_TYPE;
        this.eventVersion = eventVersion > 0 ? eventVersion : EVENT_VERSION;
        this.occurredAt = occurredAt != null ? occurredAt : Instant.now();

        // Tracing - validated
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

        // Business identity - validated
        this.sellerProfileId =
                Validator.requirePositive(
                        sellerProfileId, "sellerProfileId", "Seller profile ID must be positive");
        this.userId = Validator.requirePositive(userId, "userId", "User ID must be positive");

        // Approval details - validated
        this.approvalTier =
                Validator.requireNonNull(approvalTier, "approvalTier", "Approval tier is required");
        this.approvalSource =
                Validator.requireNonNull(
                        approvalSource, "approvalSource", "Approval source is required");
        this.commissionRate = Validator.validateCommissionRate(commissionRate);
        this.maxProductListings =
                Validator.validateMaxListings(maxProductListings, ApprovalTier.UNLIMITED_LISTINGS);
        this.reviewDuration =
                Validator.validateReviewDuration(Duration.ofSeconds(reviewDurationSeconds));
        this.verifiedDocuments = Validator.validateDocuments(verifiedDocuments);

        // Optional fields
        this.approvalNote = approvalNote;
        this.approvalConditions = approvalConditions;

        // Approver - validated
        this.approverId =
                Validator.requirePositive(
                        approverId,
                        "approverId",
                        "Approver ID is required for audit accountability");
        this.approverIdentifier =
                Validator.requireNonBlank(
                        approverIdentifier,
                        "approverIdentifier",
                        "Approver identifier is required");
    }

    // =========================================================================
    // Static Factory Methods
    // =========================================================================

    /**
     * Primary factory: Full control over all parameters. Use when you need to specify commission
     * rate or listings override.
     */
    public static SellerApprovedEvent create(
            Long sellerProfileId,
            Long userId,
            ApprovalTier tier,
            ApprovalSource approvalSource,
            Long approverId,
            String approverIdentifier,
            BigDecimal commissionRate,
            int maxProductListings,
            Duration reviewDuration,
            List<String> verifiedDocuments,
            String correlationId,
            String causationId) {

        return new SellerApprovedEvent(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                EVENT_VERSION,
                Instant.now(),
                correlationId,
                causationId,
                sellerProfileId,
                userId,
                tier,
                approvalSource,
                commissionRate,
                maxProductListings,
                reviewDuration != null ? reviewDuration.getSeconds() : 0,
                verifiedDocuments,
                null,
                null,
                approverId,
                approverIdentifier);
    }

    /** Factory: Manual admin approval using tier defaults. */
    public static SellerApprovedEvent createManualApproval(
            Long sellerProfileId,
            Long userId,
            ApprovalTier tier,
            Long approverId,
            String approverIdentifier,
            Duration reviewDuration,
            List<String> verifiedDocuments,
            String correlationId,
            String causationId) {

        return create(
                sellerProfileId,
                userId,
                tier,
                ApprovalSource.ADMIN_MANUAL,
                approverId,
                approverIdentifier,
                tier.getDefaultCommissionRate(),
                tier.getDefaultMaxProductListings(),
                reviewDuration,
                verifiedDocuments,
                correlationId,
                causationId);
    }

    /** Factory: Automated system approval (always BRONZE). */
    public static SellerApprovedEvent createAutomatedApproval(
            Long sellerProfileId,
            Long userId,
            Long systemId,
            String systemIdentifier,
            Duration reviewDuration,
            String correlationId,
            String causationId) {

        return create(
                sellerProfileId,
                userId,
                ApprovalTier.BRONZE,
                ApprovalSource.AUTOMATED_SYSTEM,
                systemId,
                systemIdentifier,
                ApprovalTier.BRONZE.getDefaultCommissionRate(),
                ApprovalTier.BRONZE.getDefaultMaxProductListings(),
                reviewDuration,
                Collections.emptyList(),
                correlationId,
                causationId);
    }

    /** Factory: Fast-track approval (standard 4-hour review). */
    public static SellerApprovedEvent createFastTrackApproval(
            Long sellerProfileId,
            Long userId,
            ApprovalTier tier,
            Long approverId,
            String approverIdentifier,
            List<String> verifiedDocuments,
            String correlationId,
            String causationId) {

        return create(
                sellerProfileId,
                userId,
                tier,
                ApprovalSource.FAST_TRACK,
                approverId,
                approverIdentifier,
                tier.getDefaultCommissionRate(),
                tier.getDefaultMaxProductListings(),
                Duration.ofHours(4),
                verifiedDocuments,
                correlationId,
                causationId);
    }

    // =========================================================================
    // Builder
    // =========================================================================

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private static final int UNSPECIFIED_LISTINGS = 0;

        private Long sellerProfileId;
        private Long userId;
        private ApprovalTier approvalTier;
        private ApprovalSource approvalSource;
        private Long approverId;
        private String approverIdentifier;
        private BigDecimal commissionRate;
        private int maxProductListings;
        private Duration reviewDuration;
        private List<String> verifiedDocuments = Collections.emptyList();
        private String approvalNote;
        private String approvalConditions;
        private String correlationId = UUID.randomUUID().toString();
        private String causationId = UUID.randomUUID().toString();

        private Builder() {}

        public Builder sellerProfileId(Long val) {
            this.sellerProfileId = val;
            return this;
        }

        public Builder userId(Long val) {
            this.userId = val;
            return this;
        }

        public Builder approvalTier(ApprovalTier val) {
            this.approvalTier = val;
            return this;
        }

        public Builder approvalSource(ApprovalSource val) {
            this.approvalSource = val;
            return this;
        }

        public Builder approverId(Long val) {
            this.approverId = val;
            return this;
        }

        public Builder approverIdentifier(String val) {
            this.approverIdentifier = val;
            return this;
        }

        public Builder commissionRate(BigDecimal val) {
            this.commissionRate = val;
            return this;
        }

        public Builder maxProductListings(int val) {
            this.maxProductListings = val;
            return this;
        }

        public Builder reviewDuration(Duration val) {
            this.reviewDuration = val;
            return this;
        }

        public Builder verifiedDocuments(List<String> val) {
            this.verifiedDocuments = val;
            return this;
        }

        public Builder approvalNote(String val) {
            this.approvalNote = val;
            return this;
        }

        public Builder approvalConditions(String val) {
            this.approvalConditions = val;
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

        public SellerApprovedEvent build() {
            BigDecimal effectiveRate =
                    (this.commissionRate != null)
                            ? this.commissionRate
                            : (this.approvalTier != null
                                    ? this.approvalTier.getDefaultCommissionRate()
                                    : null);

            int effectiveListings =
                    (this.maxProductListings != UNSPECIFIED_LISTINGS)
                            ? this.maxProductListings
                            : (this.approvalTier != null
                                    ? this.approvalTier.getDefaultMaxProductListings()
                                    : UNSPECIFIED_LISTINGS);

            Duration effectiveDuration =
                    (this.reviewDuration != null) ? this.reviewDuration : Duration.ZERO;

            return new SellerApprovedEvent(
                    UUID.randomUUID().toString(),
                    EVENT_TYPE,
                    EVENT_VERSION,
                    Instant.now(),
                    correlationId,
                    causationId,
                    sellerProfileId,
                    userId,
                    approvalTier,
                    approvalSource,
                    effectiveRate,
                    effectiveListings,
                    effectiveDuration.getSeconds(),
                    verifiedDocuments,
                    approvalNote,
                    approvalConditions,
                    approverId,
                    approverIdentifier);
        }
    }

    // =========================================================================
    // DomainEvent Contract Implementation
    // =========================================================================

    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public long getOccurredAt() {
        return occurredAt.toEpochMilli();
    }

    // Convenience accessor (avoids confusion with getOccurredAt() long)
    @JsonIgnore
    public Instant getOccurredAtInstant() {
        return occurredAt;
    }

    // =========================================================================
    // Jackson-specific Serializers for fields ignored/scoped differently
    // =========================================================================

    @JsonProperty("reviewDurationSeconds")
    public long getReviewDurationSeconds() {
        return reviewDuration != null ? reviewDuration.getSeconds() : 0;
    }

    public String getApprovalNote() {
        return approvalNote;
    }

    public Optional<String> getApprovalNoteOptional() {
        return Optional.ofNullable(approvalNote);
    }

    public String getApprovalConditions() {
        return approvalConditions;
    }

    public Optional<String> getApprovalConditionsOptional() {
        return Optional.ofNullable(approvalConditions);
    }

    // =========================================================================
    // Business Query Methods
    // =========================================================================

    public boolean isBronzeTier() {
        return approvalTier == ApprovalTier.BRONZE;
    }

    public boolean isSilverTier() {
        return approvalTier == ApprovalTier.SILVER;
    }

    public boolean isGoldTier() {
        return approvalTier == ApprovalTier.GOLD;
    }

    public boolean isPlatinumTier() {
        return approvalTier == ApprovalTier.PLATINUM;
    }

    public boolean isPremiumSeller() {
        return approvalTier == ApprovalTier.GOLD || approvalTier == ApprovalTier.PLATINUM;
    }

    public boolean isManualApproval() {
        return approvalSource == ApprovalSource.ADMIN_MANUAL
                || approvalSource == ApprovalSource.SENIOR_REVIEW
                || approvalSource == ApprovalSource.FAST_TRACK;
    }

    public boolean isAutomatedApproval() {
        return approvalSource == ApprovalSource.AUTOMATED_SYSTEM;
    }

    public boolean isFastTrackApproval() {
        return approvalSource == ApprovalSource.FAST_TRACK;
    }

    public boolean isWithinSLA() {
        return reviewDuration != null && reviewDuration.compareTo(STANDARD_SLA) <= 0;
    }

    public boolean isAutomatedReview() {
        return reviewDuration != null && reviewDuration.compareTo(MINIMUM_REVIEW_DURATION) < 0;
    }

    public boolean hasApprovalConditions() {
        return approvalConditions != null && !approvalConditions.isBlank();
    }

    public boolean wasDocumentVerified(String documentName) {
        Objects.requireNonNull(documentName, "documentName cannot be null");
        return verifiedDocuments.stream().anyMatch(doc -> doc.equalsIgnoreCase(documentName));
    }

    public boolean isForSeller(Long sellerProfileId) {
        return Objects.equals(this.sellerProfileId, sellerProfileId);
    }

    public boolean isForUser(Long userId) {
        return Objects.equals(this.userId, userId);
    }

    public boolean isRecent(long seconds) {
        if (seconds <= 0) {
            throw new DomainValidationException("seconds", "Recency window must be positive");
        }
        return Instant.now().minusSeconds(seconds).isBefore(occurredAt);
    }

    public int getVerifiedDocumentCount() {
        return verifiedDocuments.size();
    }

    public String getFormattedReviewDuration() {
        long days = reviewDuration.toDays();
        long hours = reviewDuration.toHours() % 24;
        long minutes = reviewDuration.toMinutes() % 60;

        if (days > 0) return String.format("%d days, %d hours", days, hours);
        if (hours > 0) return String.format("%d hours, %d minutes", hours, minutes);
        return String.format("%d minutes", reviewDuration.toMinutes());
    }

    public String getSLAStatus() {
        if (isWithinSLA()) {
            long hoursAhead = STANDARD_SLA.minus(reviewDuration).toHours();
            return String.format("Within SLA (%d hours ahead)", hoursAhead);
        }
        long hoursOver = reviewDuration.minus(STANDARD_SLA).toHours();
        return String.format("SLA Breach (%d hours over)", hoursOver);
    }

    // =========================================================================
    // equals & hashCode
    // =========================================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SellerApprovedEvent)) return false;
        SellerApprovedEvent that = (SellerApprovedEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    // =========================================================================
    // toString (audit-safe - excludes PII)
    // =========================================================================

    @Override
    public String toString() {
        return "SellerApprovedEvent{"
                + "eventId='"
                + eventId
                + '\''
                + ", sellerProfileId="
                + sellerProfileId
                + ", userId="
                + userId
                + ", tier="
                + approvalTier
                + ", source="
                + approvalSource
                + ", commission="
                + commissionRate
                + "%"
                + ", withinSLA="
                + isWithinSLA()
                + ", isPremium="
                + isPremiumSeller()
                + ", occurredAt="
                + occurredAt
                + ", correlationId='"
                + correlationId
                + '\''
                + '}';
    }

    // =========================================================================
    // Inner Validator (DRY - Single place for all validation logic)
    // =========================================================================

    /** Internal validation utility. Centralises all field validation to eliminate duplication. */
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

        static BigDecimal validateCommissionRate(BigDecimal rate) {
            requireNonNull(
                    rate, "commissionRate", "Commission rate is required for financial compliance");

            if (rate.compareTo(BigDecimal.ZERO) < 0) {
                throw new DomainValidationException(
                        "commissionRate", "Commission rate cannot be negative");
            }
            if (rate.compareTo(new BigDecimal("100")) > 0) {
                throw new DomainValidationException(
                        "commissionRate", "Commission rate cannot exceed 100%");
            }
            return rate;
        }

        static int validateMaxListings(int max, int unlimitedSentinel) {
            if (max != unlimitedSentinel && max <= 0) {
                throw new DomainValidationException(
                        "maxProductListings",
                        "Max product listings must be positive (or "
                                + unlimitedSentinel
                                + " for unlimited)");
            }
            return max;
        }

        static Duration validateReviewDuration(Duration duration) {
            requireNonNull(
                    duration, "reviewDuration", "Review duration is required for SLA tracking");
            if (duration.isNegative()) {
                throw new DomainValidationException(
                        "reviewDuration", "Review duration cannot be negative");
            }
            return duration;
        }

        static List<String> validateDocuments(List<String> docs) {
            if (docs == null) return Collections.emptyList();

            // Validate no null/blank entries
            boolean hasInvalid = docs.stream().anyMatch(doc -> doc == null || doc.isBlank());
            if (hasInvalid) {
                throw new DomainValidationException(
                        "verifiedDocuments", "Document list cannot contain null or blank entries");
            }

            return Collections.unmodifiableList(List.copyOf(docs));
        }
    }

    // =========================================================================
    // Nested Enums
    // =========================================================================

    /** Seller approval tiers defining commission rates, limits, and support. */
    public enum ApprovalTier {
        BRONZE("Bronze", "Entry-level", new BigDecimal("15.00"), 100, "Standard"),
        SILVER("Silver", "Established", new BigDecimal("12.00"), 500, "Priority"),
        GOLD("Gold", "High-performance", new BigDecimal("10.00"), 2_000, "Dedicated"),
        PLATINUM("Platinum", "Premium enterprise", new BigDecimal("8.00"), -1, "24/7 Premium");

        public static final int UNLIMITED_LISTINGS = -1;

        private final String label;
        private final String description;
        private final BigDecimal defaultCommissionRate;
        private final int defaultMaxProductListings;
        private final String supportLevel;

        ApprovalTier(
                String label,
                String description,
                BigDecimal defaultCommissionRate,
                int defaultMaxProductListings,
                String supportLevel) {
            this.label = label;
            this.description = description;
            this.defaultCommissionRate = defaultCommissionRate;
            this.defaultMaxProductListings = defaultMaxProductListings;
            this.supportLevel = supportLevel;
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
        public BigDecimal getDefaultCommissionRate() {
            return defaultCommissionRate;
        }

        @JsonIgnore
        public int getDefaultMaxProductListings() {
            return defaultMaxProductListings;
        }

        @JsonIgnore
        public String getSupportLevel() {
            return supportLevel;
        }

        /**
         * @return true if seller has unlimited product listings
         */
        public boolean hasUnlimitedListings() {
            return defaultMaxProductListings == UNLIMITED_LISTINGS;
        }

        /**
         * @return max listings, empty if unlimited
         */
        public Optional<Integer> getMaxListings() {
            return hasUnlimitedListings()
                    ? Optional.empty()
                    : Optional.of(defaultMaxProductListings);
        }

        /**
         * Checks if this tier is strictly higher than the other. BRONZE is NOT higher than BRONZE
         * (returns false).
         *
         * @param other tier to compare against
         * @return true if this tier has higher priority (later in enum order)
         */
        public boolean isHigherThan(ApprovalTier other) {
            Objects.requireNonNull(other, "Comparison tier cannot be null");
            return this.ordinal() > other.ordinal();
        }

        /**
         * @return next upgrade tier, empty if already PLATINUM
         */
        public Optional<ApprovalTier> getNextTier() {
            int next = this.ordinal() + 1;
            ApprovalTier[] tiers = values();
            return next < tiers.length ? Optional.of(tiers[next]) : Optional.empty();
        }
    }

    /** Source of approval decision for audit trail. */
    public enum ApprovalSource {
        ADMIN_MANUAL("Admin Manual", "Standard manual review", false),
        SENIOR_REVIEW("Senior Review", "Senior reviewer for complex cases", false),
        AUTOMATED_SYSTEM("Automated System", "Automated criteria-based approval", true),
        FAST_TRACK("Fast Track", "Expedited VIP/premium approval", false);

        private final String label;
        private final String description;
        private final boolean fullyAutomated;

        ApprovalSource(String label, String description, boolean fullyAutomated) {
            this.label = label;
            this.description = description;
            this.fullyAutomated = fullyAutomated;
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
        public boolean isFullyAutomated() {
            return fullyAutomated;
        }
    }
}
