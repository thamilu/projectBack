package com.eshop.app.user.domain.event;

import com.eshop.app.core.events.contract.DomainEvent;
import com.eshop.app.core.exception.business.DomainValidationException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable domain event published when two-factor authentication is successfully enabled for a
 * user.
 *
 * <p><b>Security Critical Event:</b> Signals a major security enhancement to user account
 * protection. 2FA significantly increases account security by requiring both password and a
 * secondary verification method.
 *
 * <p><b>Event Guarantees:</b>
 *
 * <ul>
 *   <li>Published ONLY after successful 2FA setup AND verification
 *   <li>User has successfully verified the 2FA method works
 *   <li>Backup codes have been generated for account recovery
 *   <li>2FA is immediately active and enforced after this event
 *   <li>All fields validated at construction — no partially valid instances
 *   <li>Immutable after creation — no setters, all fields final
 *   <li>Timestamp is absolute UTC — security audit trail integrity
 *   <li>EventId is unique UUID — enables idempotent processing
 * </ul>
 *
 * <p><b>Creation Contract:</b>
 *
 * <ul>
 *   <li>Use {@link #create(Long, TwoFactorMethod, String, String, String)} for standard flow
 *   <li>Use {@link #create(Long, TwoFactorMethod, String, String, String,
 *       TwoFactorEnablementSource, int, String, String)} for full context
 *   <li>Use {@link #createForSystem(Long)} for system-initiated enablement (migrations)
 *   <li>Use {@link #builder()} when infrastructure fields need explicit control
 *   <li>Direct constructor calls are not permitted — constructor is private
 * </ul>
 *
 * <p><b>2FA Setup Flow:</b>
 *
 * <pre>
 * User Initiates 2FA Setup
 *   → TwoFactorSetupStartedEvent
 *   → User configures method (scans QR / enters phone number / etc.)
 *   → User verifies code (proves setup works)
 *   → System generates backup codes
 *   → User confirms backup codes saved
 *   → TwoFactorEnabledEvent [THIS] — 2FA now enforced
 *   → UserSecurityLevelUpdatedEvent
 * </pre>
 *
 * <p><b>Downstream Consumers:</b>
 *
 * <ul>
 *   <li><b>AuthenticationService:</b> Enforce 2FA on future logins
 *   <li><b>SessionService:</b> Invalidate existing sessions (force re-auth with 2FA)
 *   <li><b>NotificationService:</b> Send "2FA enabled" confirmation email
 *   <li><b>SecurityAuditService:</b> Log 2FA enablement with full context
 *   <li><b>ComplianceService:</b> Update compliance status (GDPR, PCI DSS)
 *   <li><b>TrustService:</b> Increase account trust score
 *   <li><b>AnalyticsService:</b> Track 2FA adoption metrics
 * </ul>
 *
 * <p><b>Serialization Contract:</b>
 *
 * <ul>
 *   <li>Jackson deserializes via {@link JsonCreator} private constructor
 *   <li>All fields serialized with explicit {@link JsonProperty} names
 *   <li>Computed boolean methods annotated {@link JsonIgnore} — not serialized
 *   <li>Enum getters annotated {@link JsonIgnore} — enums serialize as name string
 * </ul>
 *
 * <p><b>Security:</b>
 *
 * <ul>
 *   <li>TOTP secret never included — stored separately, encrypted
 *   <li>Backup codes never included — stored separately, encrypted
 *   <li>{@code ipAddress} excluded from {@link #toString()} — security audit logs only
 *   <li>{@code ipAddress} value excluded from all error messages
 * </ul>
 *
 * @see DomainEvent
 * @see TwoFactorDisabledEvent
 * @version 1.0
 * @since 1.0
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public final class TwoFactorEnabledEvent implements DomainEvent {

    // =========================================================================
    // Constants — Event Identity
    // =========================================================================

    /**
     * Event type identifier — used for event routing and polymorphic handling. Stable across schema
     * versions to avoid consumer breakage.
     */
    public static final String EVENT_TYPE = "user.two_factor.enabled";

    /**
     * Current event schema version. Increment on breaking structural changes; add migration
     * handler.
     */
    public static final int EVENT_VERSION = 1;

    /**
     * Default number of backup codes generated per 2FA enablement. Industry standard is 8-10
     * single-use recovery codes.
     */
    public static final int DEFAULT_BACKUP_CODES_COUNT = 10;

    /**
     * Maximum allowed backup codes count. Upper bound prevents unreasonably large recovery code
     * sets.
     */
    public static final int MAX_BACKUP_CODES = 100;

    // =========================================================================
    // Event Identity & Infrastructure Metadata
    // =========================================================================

    /**
     * Unique event identifier (UUID v4). Enables idempotent processing — prevents double 2FA
     * enablement on replay.
     */
    @JsonProperty("eventId")
    private final String eventId;

    /**
     * Event type identifier — always {@value EVENT_TYPE} for this class. Stored in the event
     * payload for schema-on-read deserialization support.
     */
    @JsonProperty("eventType")
    private final String eventType;

    /**
     * Event schema version — always {@value EVENT_VERSION} for current schema. Consumers check this
     * to apply the correct deserialization strategy.
     */
    @JsonProperty("eventVersion")
    private final int eventVersion;

    /**
     * Absolute UTC timestamp when 2FA enablement occurred.
     *
     * <p><b>Security Critical:</b> Used for security audit trail and compliance. Proves when 2FA
     * was enabled — essential for incident investigation.
     */
    @JsonProperty("occurredAt")
    private final Instant occurredAt;

    // =========================================================================
    // Distributed Tracing Identifiers
    // =========================================================================

    /**
     * Correlation ID linking all events within a single 2FA setup flow. Propagated from the inbound
     * request (MDC/header) or generated fresh.
     */
    @JsonProperty("correlationId")
    private final String correlationId;

    /**
     * Causation ID identifying what triggered this 2FA enablement. Example:
     * TwoFactorSetupStartedEvent → TwoFactorEnabledEvent.
     */
    @JsonProperty("causationId")
    private final String causationId;

    // =========================================================================
    // User & 2FA Method Data
    // =========================================================================

    /**
     * User ID who enabled 2FA (primary key of User aggregate). Must be positive — set after
     * database persistence.
     */
    @JsonProperty("userId")
    private final Long userId;

    /**
     * 2FA method that was enabled.
     *
     * <p>Security characteristics per method:
     *
     * <ul>
     *   <li>TOTP: Score 90 — highest security, offline-capable
     *   <li>BIOMETRIC: Score 85 — high security, excellent UX, device-dependent
     *   <li>SMS: Score 60 — medium security, vulnerable to SIM swapping
     *   <li>EMAIL: Score 50 — lower security, always available
     * </ul>
     */
    @JsonProperty("twoFactorMethod")
    private final TwoFactorMethod twoFactorMethod;

    // =========================================================================
    // Security Context
    // =========================================================================

    /**
     * IP address from which 2FA was enabled (validated IPv4 or IPv6).
     *
     * <p>Used for security audit and fraud detection. Can detect unauthorized 2FA enablement from
     * suspicious IPs or attack patterns.
     *
     * <p><b>Security:</b> Excluded from {@link #toString()}. Include only in dedicated security
     * audit logs, never in standard application logs.
     */
    @JsonProperty("ipAddress")
    private final String ipAddress;

    /**
     * Geographic location of 2FA enablement (if available). Used for security analysis and anomaly
     * detection.
     *
     * <p>Null when geolocation is unavailable. Example: {@code "US, California"}.
     */
    @JsonProperty("location")
    private final String location;

    /**
     * Device fingerprint/user agent of enablement source. Helps identify if 2FA was enabled from
     * user's normal device.
     *
     * <p>Null when user agent is unavailable. Example: {@code "Mozilla/5.0 (Windows NT 10.0; Win64;
     * x64)..."}.
     *
     * <p><b>Security:</b> Excluded from {@link #toString()} — security audit logs only.
     */
    @JsonProperty("userAgent")
    private final String userAgent;

    /** Who initiated the 2FA enablement. Different compliance requirements apply per source. */
    @JsonProperty("enablementSource")
    private final TwoFactorEnablementSource enablementSource;

    /**
     * Number of backup codes generated. Proves recovery method exists. Validated: 1 to {@value
     * MAX_BACKUP_CODES}. Backup code values are stored separately, encrypted — never in the event.
     */
    @JsonProperty("backupCodesCount")
    private final int backupCodesCount;

    // =========================================================================
    // Single Private Constructor — Validation Exactly Once
    // =========================================================================

    /**
     * Single validated constructor. All field assignment and validation occurs here.
     *
     * <p>Annotated with {@link JsonCreator} for Jackson deserialization support. Standard Jackson
     * 2.x accesses private constructors via reflection. Validation is strict — no silent fallbacks
     * for any field.
     *
     * @throws DomainValidationException if any field fails validation
     */
    @JsonCreator
    private TwoFactorEnabledEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("eventVersion") int eventVersion,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("causationId") String causationId,
            @JsonProperty("userId") Long userId,
            @JsonProperty("twoFactorMethod") TwoFactorMethod twoFactorMethod,
            @JsonProperty("ipAddress") String ipAddress,
            @JsonProperty("location") String location,
            @JsonProperty("userAgent") String userAgent,
            @JsonProperty("enablementSource") TwoFactorEnablementSource enablementSource,
            @JsonProperty("backupCodesCount") int backupCodesCount) {

        // Infrastructure fields — strict, no silent fallbacks
        this.eventId = EventValidator.requireNonBlank(eventId, "eventId", "Event ID is required");
        this.eventType =
                EventValidator.requireNonBlank(eventType, "eventType", "Event type is required");
        this.eventVersion =
                EventValidator.requirePositiveInt(
                        eventVersion, "eventVersion", "Event version must be positive");
        this.occurredAt =
                EventValidator.requireNonNull(
                        occurredAt, "occurredAt", "Event timestamp is required");

        // Distributed tracing identifiers
        this.correlationId =
                EventValidator.requireNonBlank(
                        correlationId,
                        "correlationId",
                        "Correlation ID is required for distributed tracing");
        this.causationId =
                EventValidator.requireNonBlank(
                        causationId,
                        "causationId",
                        "Causation ID is required to track what triggered 2FA enablement");

        // Domain fields — full validation via EventValidator (DRY)
        this.userId =
                EventValidator.requirePositiveLong(
                        userId, "userId", "User ID is required and must be positive");
        this.twoFactorMethod =
                EventValidator.requireNonNull(
                        twoFactorMethod, "twoFactorMethod", "Two-factor method is required");
        this.ipAddress =
                EventValidator.validateIpAddress(
                        ipAddress, "IP address is required for security audit");
        this.enablementSource =
                EventValidator.requireNonNull(
                        enablementSource, "enablementSource", "Enablement source is required");
        this.backupCodesCount = Validator.validateBackupCodesCount(backupCodesCount);

        // Optional context fields — normalized via EventValidator (DRY)
        this.location = EventValidator.normalizeOptional(location);
        this.userAgent = EventValidator.normalizeOptional(userAgent);
    }

    // =========================================================================
    // Factory Methods — Intended Public Creation API
    // =========================================================================

    /**
     * Creates a standard user-initiated 2FA enablement event.
     *
     * <p>Defaults applied:
     *
     * <ul>
     *   <li>{@code enablementSource} = {@link TwoFactorEnablementSource#USER_INITIATED}
     *   <li>{@code backupCodesCount} = {@value DEFAULT_BACKUP_CODES_COUNT}
     *   <li>{@code location} = null
     *   <li>{@code userAgent} = null
     * </ul>
     *
     * @param userId persisted user ID — must be positive
     * @param method 2FA method — must be non-null
     * @param ipAddress IP address where 2FA was enabled — valid IPv4/IPv6
     * @param correlationId correlation ID from request context
     * @param causationId causation ID from triggering command/event
     * @return validated, immutable event
     * @throws DomainValidationException if any required field is invalid
     */
    public static TwoFactorEnabledEvent create(
            Long userId,
            TwoFactorMethod method,
            String ipAddress,
            String correlationId,
            String causationId) {

        return create(
                userId,
                method,
                ipAddress,
                correlationId,
                causationId,
                TwoFactorEnablementSource.USER_INITIATED,
                DEFAULT_BACKUP_CODES_COUNT,
                null,
                null);
    }

    /**
     * Creates a fully contextualized 2FA enablement event.
     *
     * @param userId persisted user ID — must be positive
     * @param method 2FA method — must be non-null
     * @param ipAddress IP address — must be valid IPv4 or IPv6
     * @param correlationId correlation ID from request context
     * @param causationId causation ID from triggering command/event
     * @param enablementSource who initiated the enablement — must be non-null
     * @param backupCodesCount backup codes generated — must be 1 to {@value MAX_BACKUP_CODES}
     * @param location optional geographic location — nullable
     * @param userAgent optional device user agent — nullable
     * @return validated, immutable event
     * @throws DomainValidationException if any required field is invalid
     */
    public static TwoFactorEnabledEvent create(
            Long userId,
            TwoFactorMethod method,
            String ipAddress,
            String correlationId,
            String causationId,
            TwoFactorEnablementSource enablementSource,
            int backupCodesCount,
            String location,
            String userAgent) {

        return builder()
                .userId(userId)
                .twoFactorMethod(method)
                .ipAddress(ipAddress)
                .correlationId(correlationId)
                .causationId(causationId)
                .enablementSource(enablementSource)
                .backupCodesCount(backupCodesCount)
                .location(location)
                .userAgent(userAgent)
                .build();
    }

    /**
     * Creates an event for system-originated 2FA enablement (migrations, automation).
     *
     * <p>Applies system defaults:
     *
     * <ul>
     *   <li>{@code twoFactorMethod} = {@link TwoFactorMethod#TOTP}
     *   <li>{@code ipAddress} = {@code "127.0.0.1"} (loopback)
     *   <li>{@code enablementSource} = {@link TwoFactorEnablementSource#SYSTEM_MIGRATION}
     *   <li>{@code backupCodesCount} = {@value DEFAULT_BACKUP_CODES_COUNT}
     *   <li>{@code location} = null, {@code userAgent} = null
     * </ul>
     *
     * @param userId persisted user ID — must be positive
     * @return validated, immutable event with system defaults
     * @throws DomainValidationException if userId is invalid
     */
    public static TwoFactorEnabledEvent createForSystem(Long userId) {
        return builder()
                .userId(userId)
                .twoFactorMethod(TwoFactorMethod.TOTP)
                .ipAddress("127.0.0.1")
                .enablementSource(TwoFactorEnablementSource.SYSTEM_MIGRATION)
                .backupCodesCount(DEFAULT_BACKUP_CODES_COUNT)
                .build();
    }

    // =========================================================================
    // Builder
    // =========================================================================

    /**
     * Returns a new {@link Builder} for constructing a {@code TwoFactorEnabledEvent}.
     *
     * <p>Use when infrastructure fields need explicit control: propagating existing correlation
     * IDs, event replay, or testing. For standard use, prefer the factory methods.
     *
     * @return new Builder with infrastructure field defaults applied
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a pre-populated {@link Builder} seeded from this event.
     *
     * <p>Used for creating modified copies in event sourcing replay and testing. {@code eventType}
     * and {@code eventVersion} are intentionally omitted — they are type-level constants fixed in
     * {@link Builder#build()}.
     *
     * @return builder with all non-constant fields copied from this event
     */
    public Builder toBuilder() {
        return new Builder()
                .eventId(this.eventId)
                .occurredAt(this.occurredAt)
                .correlationId(this.correlationId)
                .causationId(this.causationId)
                .userId(this.userId)
                .twoFactorMethod(this.twoFactorMethod)
                .ipAddress(this.ipAddress)
                .location(this.location)
                .userAgent(this.userAgent)
                .enablementSource(this.enablementSource)
                .backupCodesCount(this.backupCodesCount);
    }

    /**
     * Fluent builder for {@link TwoFactorEnabledEvent}.
     *
     * <p><b>Infrastructure defaults (auto-generated, overridable):</b>
     *
     * <ul>
     *   <li>{@code eventId} — UUID v4
     *   <li>{@code occurredAt} — {@code Instant.now()} at builder creation
     *   <li>{@code correlationId} — UUID v4
     *   <li>{@code causationId} — UUID v4
     * </ul>
     *
     * <p><b>Fixed values (hardcoded in {@link #build()}):</b>
     *
     * <ul>
     *   <li>{@code eventType} — always {@link TwoFactorEnabledEvent#EVENT_TYPE}
     *   <li>{@code eventVersion} — always {@link TwoFactorEnabledEvent#EVENT_VERSION}
     * </ul>
     *
     * <p><b>Domain fields (no defaults — must be explicitly set):</b> {@code userId}, {@code
     * twoFactorMethod}, {@code ipAddress}, {@code enablementSource}, {@code backupCodesCount}.
     *
     * <p>Validation occurs in the constructor — single validation path regardless of creation
     * route.
     */
    public static final class Builder {

        // Infrastructure — overridable for replay/testing
        private String eventId = UUID.randomUUID().toString();
        private Instant occurredAt = Instant.now();
        private String correlationId = UUID.randomUUID().toString();
        private String causationId = UUID.randomUUID().toString();

        // Domain — no defaults, must be explicitly set
        private Long userId;
        private TwoFactorMethod twoFactorMethod;
        private String ipAddress;
        private TwoFactorEnablementSource enablementSource;
        private int backupCodesCount;

        // Optional context — nullable
        private String location;
        private String userAgent;

        private Builder() {}

        /** Overrides auto-generated event ID. Use for replay or testing. */
        public Builder eventId(String val) {
            this.eventId = val;
            return this;
        }

        /** Overrides auto-generated timestamp. Use for replay or testing. */
        public Builder occurredAt(Instant val) {
            this.occurredAt = val;
            return this;
        }

        /** Sets correlation ID propagated from request context (MDC/tracing header). */
        public Builder correlationId(String val) {
            this.correlationId = val;
            return this;
        }

        /** Sets causation ID from triggering command or event. */
        public Builder causationId(String val) {
            this.causationId = val;
            return this;
        }

        /** Sets persisted user ID — must be non-null and positive. */
        public Builder userId(Long val) {
            this.userId = val;
            return this;
        }

        /** Sets 2FA method — must be non-null. */
        public Builder twoFactorMethod(TwoFactorMethod val) {
            this.twoFactorMethod = val;
            return this;
        }

        /** Sets IP address — validated as IPv4 or IPv6 at build time. */
        public Builder ipAddress(String val) {
            this.ipAddress = val;
            return this;
        }

        /** Sets optional geographic location — nullable, trimmed if present. */
        public Builder location(String val) {
            this.location = val;
            return this;
        }

        /** Sets optional device user agent — nullable, trimmed if present. */
        public Builder userAgent(String val) {
            this.userAgent = val;
            return this;
        }

        /** Sets enablement source — must be non-null. */
        public Builder enablementSource(TwoFactorEnablementSource val) {
            this.enablementSource = val;
            return this;
        }

        /** Sets backup codes count — must be between 1 and {@value MAX_BACKUP_CODES}. */
        public Builder backupCodesCount(int val) {
            this.backupCodesCount = val;
            return this;
        }

        /**
         * Constructs and validates the {@link TwoFactorEnabledEvent}.
         *
         * <p>{@code eventType} and {@code eventVersion} injected as constants — builder cannot
         * override them. All validation delegated to constructor.
         *
         * @return validated, immutable event
         * @throws DomainValidationException if any required field is missing or invalid
         */
        public TwoFactorEnabledEvent build() {
            return new TwoFactorEnabledEvent(
                    eventId,
                    EVENT_TYPE, // fixed constant — builder cannot override
                    EVENT_VERSION, // fixed constant — builder cannot override
                    occurredAt,
                    correlationId,
                    causationId,
                    userId,
                    twoFactorMethod,
                    ipAddress,
                    location,
                    userAgent,
                    enablementSource,
                    backupCodesCount);
        }
    }

    // =========================================================================
    // Validator — DRY, Stateless, Single Responsibility, Package-Private
    // =========================================================================

    /**
     * Stateless field validators for {@link TwoFactorEnabledEvent}.
     *
     * <p><b>Design principles:</b>
     *
     * <ul>
     *   <li><b>DRY:</b> Each validation rule defined exactly once
     *   <li><b>SRP:</b> Validation logic isolated from event state
     *   <li><b>Testability:</b> Package-private — unit-testable independently
     * </ul>
     *
     * <p><b>Privacy contract:</b> Field values never included in exception messages. This applies
     * especially to IP addresses — security-sensitive data.
     */
    static final class Validator {

        private Validator() {
            throw new UnsupportedOperationException("Utility class — do not instantiate");
        }

        /**
         * Validates backup codes count: must be between 1 and {@value MAX_BACKUP_CODES}.
         *
         * @param count candidate count
         * @return validated count
         * @throws DomainValidationException if out of valid range
         */
        static int validateBackupCodesCount(int count) {
            if (count <= 0) {
                throw new DomainValidationException(
                        "backupCodesCount",
                        "At least one backup code must be generated for account recovery");
            }
            if (count > MAX_BACKUP_CODES) {
                throw new DomainValidationException(
                        "backupCodesCount",
                        "Backup codes count exceeds maximum of " + MAX_BACKUP_CODES);
            }
            return count;
        }
    }

    // =========================================================================
    // DomainEvent Interface Implementation
    // =========================================================================

    /** {@inheritDoc} Returns {@link #EVENT_TYPE} — constant for this event class. */
    @Override
    public String getEventType() {
        return eventType;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns epoch milliseconds. Use {@link #getOccurredAtInstant()} when {@link Instant}
     * precision is needed.
     */
    @Override
    public long getOccurredAt() {
        return occurredAt.toEpochMilli();
    }

    /** {@inheritDoc} Returns correlation ID for distributed tracing propagation. */
    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    // =========================================================================
    // Accessors
    // =========================================================================

    /**
     * @return unique event identifier (UUID v4)
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * @return event schema version
     */
    public int getEventVersion() {
        return eventVersion;
    }

    /**
     * Returns event timestamp as {@link Instant}.
     *
     * <p>Prefer this over {@link #getOccurredAt()} when working with {@link java.time} APIs or when
     * sub-millisecond precision matters.
     *
     * @return event timestamp (UTC)
     */
    public Instant getOccurredAtInstant() {
        return occurredAt;
    }

    /**
     * @return causation ID identifying the triggering command or event
     */
    public String getCausationId() {
        return causationId;
    }

    /**
     * @return user ID who enabled 2FA
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * @return 2FA method that was enabled
     */
    public TwoFactorMethod getTwoFactorMethod() {
        return twoFactorMethod;
    }

    /**
     * Returns the IP address from which 2FA was enabled.
     *
     * <p><b>Security:</b> Include only in dedicated security audit logs. Never log in standard
     * application logs.
     *
     * @return validated IPv4 or IPv6 address
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Returns geographic location of 2FA enablement.
     *
     * @return location string (e.g., {@code "US, California"}), or null if unavailable
     */
    public String getLocation() {
        return location;
    }

    /**
     * Returns device user agent string.
     *
     * <p><b>Security:</b> Include only in dedicated security audit logs.
     *
     * @return user agent string, or null if unavailable
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * @return who initiated the 2FA enablement
     */
    public TwoFactorEnablementSource getEnablementSource() {
        return enablementSource;
    }

    /**
     * @return number of backup codes generated
     */
    public int getBackupCodesCount() {
        return backupCodesCount;
    }

    // =========================================================================
    // Business Query Methods
    // =========================================================================

    /**
     * Whether the 2FA method is TOTP (authenticator app).
     *
     * @return true if {@code twoFactorMethod} is {@link TwoFactorMethod#TOTP}
     */
    @JsonIgnore
    public boolean isTotpMethod() {
        return twoFactorMethod == TwoFactorMethod.TOTP;
    }

    /**
     * Whether the 2FA method is SMS.
     *
     * @return true if {@code twoFactorMethod} is {@link TwoFactorMethod#SMS}
     */
    @JsonIgnore
    public boolean isSmsMethod() {
        return twoFactorMethod == TwoFactorMethod.SMS;
    }

    /**
     * Whether the 2FA method is EMAIL.
     *
     * @return true if {@code twoFactorMethod} is {@link TwoFactorMethod#EMAIL}
     */
    @JsonIgnore
    public boolean isEmailMethod() {
        return twoFactorMethod == TwoFactorMethod.EMAIL;
    }

    /**
     * Whether the 2FA method is BIOMETRIC.
     *
     * @return true if {@code twoFactorMethod} is {@link TwoFactorMethod#BIOMETRIC}
     */
    @JsonIgnore
    public boolean isBiometricMethod() {
        return twoFactorMethod == TwoFactorMethod.BIOMETRIC;
    }

    /**
     * Whether this is a high-security 2FA method.
     *
     * <p>Delegates to {@link TwoFactorMethod#isHighSecurity()} — security classification is owned
     * by the enum, not the event.
     *
     * @return true if twoFactorMethod security score is above 80
     */
    @JsonIgnore
    public boolean isHighSecurity() {
        return twoFactorMethod.isHighSecurity();
    }

    /**
     * Whether enablement was user-initiated (voluntary).
     *
     * @return true if {@code enablementSource} is {@link TwoFactorEnablementSource#USER_INITIATED}
     */
    @JsonIgnore
    public boolean isUserInitiated() {
        return enablementSource == TwoFactorEnablementSource.USER_INITIATED;
    }

    /**
     * Whether enablement was admin-enforced (compliance/policy).
     *
     * @return true if {@code enablementSource} is {@link TwoFactorEnablementSource#ADMIN_ENFORCED}
     */
    @JsonIgnore
    public boolean isAdminEnforced() {
        return enablementSource == TwoFactorEnablementSource.ADMIN_ENFORCED;
    }

    /**
     * Whether enablement was triggered by system migration or automation.
     *
     * @return true if {@code enablementSource} is {@link
     *     TwoFactorEnablementSource#SYSTEM_MIGRATION}
     */
    @JsonIgnore
    public boolean isSystemMigration() {
        return enablementSource == TwoFactorEnablementSource.SYSTEM_MIGRATION;
    }

    /**
     * Whether this event concerns a specific user.
     *
     * @param targetUserId user ID to match
     * @return true if this event's {@code userId} equals {@code targetUserId}
     */
    @JsonIgnore
    public boolean isForUser(Long targetUserId) {
        return Objects.equals(this.userId, targetUserId);
    }

    /**
     * Whether event occurred within the last {@code seconds} seconds.
     *
     * @param seconds positive number of seconds defining the recency window
     * @return true if event occurred after {@code (Instant.now() - seconds)}
     * @throws DomainValidationException if seconds is zero or negative
     */
    @JsonIgnore
    public boolean isRecent(long seconds) {
        if (seconds <= 0) {
            throw new DomainValidationException("seconds", "Recency window must be positive");
        }
        return Instant.now().minusSeconds(seconds).isBefore(occurredAt);
    }

    /**
     * Security score improvement provided by this 2FA method (0–100 scale).
     *
     * @return security score
     */
    @JsonIgnore
    public int getSecurityScore() {
        return twoFactorMethod.getSecurityScore();
    }

    /**
     * Recovery difficulty if the user loses access to their 2FA device.
     *
     * @return recovery difficulty level
     */
    @JsonIgnore
    public RecoveryDifficulty getRecoveryDifficulty() {
        return twoFactorMethod.getRecoveryDifficulty();
    }

    // =========================================================================
    // Object Contract
    // =========================================================================

    /**
     * Two events are equal if and only if they have the same {@code eventId}. Enables deduplication
     * in Sets/Maps and idempotent event store processing.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TwoFactorEnabledEvent other)) {
            return false;
        }
        return Objects.equals(eventId, other.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    /**
     * Log-safe string representation.
     *
     * <p><b>Security:</b> Excludes {@code ipAddress} and {@code userAgent} from standard output.
     */
    @Override
    public String toString() {
        return "TwoFactorEnabledEvent{"
                + "eventId='"
                + eventId
                + '\''
                + ", eventType='"
                + eventType
                + '\''
                + ", eventVersion="
                + eventVersion
                + ", userId="
                + userId
                + ", twoFactorMethod="
                + twoFactorMethod
                + ", enablementSource="
                + enablementSource
                + ", backupCodesCount="
                + backupCodesCount
                + ", occurredAt="
                + occurredAt
                + ", correlationId='"
                + correlationId
                + '\''
                + ", causationId='"
                + causationId
                + '\''
                + '}';
    }

    // =========================================================================
    // Nested Enums
    // =========================================================================

    /** Enumeration of 2FA methods supported by the system. */
    public enum TwoFactorMethod {
        TOTP(
                "TOTP",
                "Time-based One-Time Password (Authenticator App)",
                "Google Authenticator, Microsoft Authenticator, Authy, etc.",
                90,
                RecoveryDifficulty.MEDIUM),
        SMS(
                "SMS",
                "Short Message Service (Text Message)",
                "Code sent to registered phone number",
                60,
                RecoveryDifficulty.LOW),
        EMAIL(
                "Email",
                "Email-based Verification",
                "Code sent to registered email address",
                50,
                RecoveryDifficulty.LOW),
        BIOMETRIC(
                "Biometric",
                "Biometric Authentication",
                "Fingerprint or face recognition",
                85,
                RecoveryDifficulty.HIGH);

        private final String label;
        private final String description;
        private final String examples;
        private final int securityScore;
        private final RecoveryDifficulty recoveryDifficulty;

        TwoFactorMethod(
                String label,
                String description,
                String examples,
                int securityScore,
                RecoveryDifficulty recoveryDifficulty) {
            this.label = label;
            this.description = description;
            this.examples = examples;
            this.securityScore = securityScore;
            this.recoveryDifficulty = recoveryDifficulty;
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
        public String getExamples() {
            return examples;
        }

        @JsonIgnore
        public int getSecurityScore() {
            return securityScore;
        }

        @JsonIgnore
        public RecoveryDifficulty getRecoveryDifficulty() {
            return recoveryDifficulty;
        }

        @JsonIgnore
        public boolean isHighSecurity() {
            return securityScore > 80;
        }
    }

    /** Enumeration of 2FA enablement sources. */
    public enum TwoFactorEnablementSource {
        USER_INITIATED("User Initiated", "User voluntarily enabled 2FA for enhanced security"),
        ADMIN_ENFORCED(
                "Admin Enforced", "Administrator enforced 2FA for security policy compliance"),
        SYSTEM_MIGRATION(
                "System Migration",
                "System automatically enabled 2FA during migration or compliance enforcement");

        private final String label;
        private final String description;

        TwoFactorEnablementSource(String label, String description) {
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

    /** Recovery difficulty if user loses their 2FA device. */
    public enum RecoveryDifficulty {
        LOW("Low", "Easy to recover account"),
        MEDIUM("Medium", "Can recover with backup codes"),
        HIGH("High", "Difficult to recover without backup codes");

        private final String label;
        private final String description;

        RecoveryDifficulty(String label, String description) {
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
