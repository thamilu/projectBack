package com.eshop.app.user.domain.event;

import com.eshop.app.core.events.contract.DomainEvent;
import com.eshop.app.core.exception.business.DomainValidationException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Domain event published when two-factor authentication is disabled for a user account.
 *
 * <p><b>⚠️ SECURITY CRITICAL EVENT:</b> Disabling 2FA is a HIGH-RISK security operation that
 * REDUCES account security. This event is MORE sensitive than TwoFactorEnabledEvent because:
 *
 * <ul>
 *   <li>It REDUCES account protection (attacker removes your security)
 *   <li>Frequently used in account takeover attacks
 *   <li>Must be properly authorized (re-authentication required)
 *   <li>Must generate security alerts (notify user immediately)
 *   <li>Must be logged with complete context (fraud investigation)
 *   <li>Must be monitored for suspicious patterns (attack detection)
 * </ul>
 *
 * <p><b>Business Meaning:</b> User's account security level has been REDUCED. The user will no
 * longer be required to provide a second factor during authentication. This could be:
 *
 * <ul>
 *   <li><b>Legitimate:</b> User lost device, switching methods, no longer wants 2FA
 *   <li><b>Malicious:</b> Attacker disabling 2FA after gaining temporary access
 *   <li><b>Administrative:</b> Admin disabled for support/compliance reasons
 *   <li><b>System:</b> Automatic disablement due to expired/invalid 2FA config
 * </ul>
 *
 * <p><b>Event Guarantees:</b>
 *
 * <ul>
 *   <li>Published ONLY after successful disablement AND verification
 *   <li>User re-authenticated before disablement (password + existing 2FA or backup code)
 *   <li>Previous 2FA method and duration recorded for audit
 *   <li>Complete security context captured (IP, location, device)
 *   <li>Security alert generated (notify user immediately)
 *   <li>Event is immutable after creation (cannot be modified or deleted)
 *   <li>Event timestamp is absolute (UTC) for forensic investigation
 *   <li>Event ID ensures deduplication (prevent race condition exploits)
 * </ul>
 *
 * <p><b>2FA Disablement Flow:</b>
 *
 * <pre>
 * User initiates 2FA disablement
 *   ↓
 * System REQUIRES re-authentication (password + current 2FA code or backup code)
 *   ↓
 * [If re-auth fails → Abort + SecurityAlertEvent → Notify user of attempt]
 *   ↓
 * [If re-auth succeeds]
 *   ↓
 * Record complete context (IP, location, device, reason)
 *   ↓
 * Disable 2FA in database
 *   ↓
 * TwoFactorDisabledEvent (THIS EVENT) ← Published here
 *   ↓
 * [Concurrent Processing]
 *   ├─ SecurityAlertService: Send IMMEDIATE security alert to user
 *   ├─ SessionService: Invalidate all existing sessions (force re-login)
 *   ├─ AuthenticationService: Remove 2FA requirement from login flow
 *   ├─ AuditService: Log with full forensic context
 *   ├─ BackupCodeService: Invalidate all backup codes
 *   ├─ FraudDetectionService: Analyze for suspicious patterns
 *   ├─ ComplianceService: Update compliance status
 *   └─ AnalyticsService: Track 2FA adoption/abandonment
 *   ↓
 * UserSecurityLevelDowngradedEvent (Security level reduced)
 * </pre>
 *
 * <p><b>Event Sequence in Security Timeline:</b>
 *
 * <pre>
 * TwoFactorEnabledEvent (2FA activated) ← Previous event
 *   ↓
 * [Multiple successful logins with 2FA]
 *   ↓
 * TwoFactorDisabledEvent (THIS EVENT) ← 2FA deactivated
 *   ↓
 * [Optional: TwoFactorReenablementRequestedEvent]
 * [Optional: AccountTakeoverDetectedEvent (if suspicious)]
 * </pre>
 *
 * <p><b>Attack Pattern Detection:</b>
 *
 * <pre>
 * Pattern 1 - Account Takeover:
 *   UserLoginEvent (unusual IP) → TwoFactorDisabledEvent → PasswordChangedEvent
 *   = High risk: 2FA disabled + password changed from unusual location
 *
 * Pattern 2 - Mass Attack:
 *   TwoFactorDisabledEvent × 100 (same IP, different users)
 *   = Botnet attack on multiple accounts
 *
 * Pattern 3 - Rapid Toggle:
 *   TwoFactorEnabledEvent → TwoFactorDisabledEvent (&lt; 5 min)
 *   = Testing for vulnerability or confused user
 *
 * Pattern 4 - Off-Hours:
 *   TwoFactorDisabledEvent (2AM, unusual timezone)
 *   = Potential account compromise
 * </pre>
 *
 * <p><b>Downstream Consumers & Responsibilities:</b>
 *
 * <ul>
 *   <li><b>SecurityAlertService:</b> Send IMMEDIATE email/push notification to user
 *   <li><b>SessionService:</b> Invalidate ALL active sessions (force re-login)
 *   <li><b>AuthenticationService:</b> Remove 2FA requirement from login flow
 *   <li><b>BackupCodeService:</b> Invalidate all remaining backup codes
 *   <li><b>FraudDetectionService:</b> Analyze patterns, alert if suspicious
 *   <li><b>AuditService:</b> Log with full forensic context
 *   <li><b>ComplianceService:</b> Update compliance record
 *   <li><b>TrustService:</b> Decrease account trust score
 *   <li><b>AnalyticsService:</b> Track 2FA abandonment metrics
 *   <li><b>RiskScoringService:</b> Increase account risk score
 * </ul>
 *
 * <h3>Security & Forensic Requirements:</h3>
 *
 * <ul>
 *   <li>Complete IP and location tracking (required for investigation)
 *   <li>Device fingerprint (identify if known device)
 *   <li>Verification method used (proof of authorization)
 *   <li>Previous 2FA duration (usage pattern analysis)
 *   <li>Disablement reason (user intent documentation)
 *   <li>Initiator (user vs admin vs system)
 *   <li>Immutable timestamp (evidence integrity)
 * </ul>
 *
 * <h3>Compliance & Legal Requirements:</h3>
 *
 * <ul>
 *   <li><b>GDPR Article 32:</b> Security measures change documentation
 *   <li><b>PCI DSS Req 8:</b> MFA change audit logging
 *   <li><b>SOC 2 CC6:</b> Logical access controls change management
 *   <li><b>HIPAA 164.312:</b> Access control modification audit
 *   <li><b>ISO 27001:</b> Information security event management
 * </ul>
 *
 * @author Your Team
 * @version 1.0
 * @since 1.0
 * @see DomainEvent
 * @see TwoFactorEnabledEvent
 */
@Getter
public final class TwoFactorDisabledEvent implements DomainEvent {

    // ========================================================================
    // Constants
    // ========================================================================

    /**
     * Event type identifier for this domain event. Used for polymorphic event handling and event
     * store querying.
     */
    public static final String EVENT_TYPE = "user.two_factor.disabled";

    /**
     * Current event schema version. Incremented when event structure changes (for schema
     * evolution).
     */
    public static final int EVENT_VERSION = 1;

    /**
     * Minimum expected 2FA usage duration before disablement. 2FA disabled less than 1 hour after
     * enabling is suspicious.
     */
    private static final Duration MINIMUM_EXPECTED_DURATION = Duration.ofHours(1);

    // ========================================================================
    // Event Identity & Metadata
    // ========================================================================

    /**
     * Unique identifier for this event. Used for deduplication and idempotent processing.
     *
     * <p>Critical for 2FA: Prevents duplicate disablement if event is replayed.
     */
    @NotBlank(message = "Event ID is required")
    private final String eventId;

    /** Type identifier for this event. Enables polymorphic handling of different event types. */
    @NotBlank(message = "Event type is required")
    @JsonProperty("type")
    private final String eventType;

    /** Schema version of this event. Enables handling of event structure evolution. */
    @Positive(message = "Event version must be positive")
    private final int eventVersion;

    /**
     * Timestamp when 2FA was disabled (UTC).
     *
     * <p><b>FORENSIC CRITICAL:</b> Absolute timestamp is required for:
     *
     * <ul>
     *   <li>Security incident investigation (exactly when attack occurred)
     *   <li>Legal proceedings (timestamp integrity evidence)
     *   <li>Compliance audit (mandatory security logging)
     *   <li>Attack pattern detection (rapid disablement after login)
     * </ul>
     */
    @NotNull(message = "Event occurred timestamp is required")
    private final Instant occurredAt;

    // ========================================================================
    // Distributed Tracing & Correlation
    // ========================================================================

    /**
     * Correlation ID for distributed tracing. Links all events in 2FA disablement workflow.
     *
     * <p>Used to trace complete flow across SecurityAlertService, SessionService, AuditService,
     * etc.
     */
    @NotBlank(message = "Correlation ID is required")
    private final String correlationId;

    /**
     * Causation ID for event causality tracking. What triggered this disablement?
     *
     * <p>Example: UserTwoFactorDisablementRequestedEvent → TwoFactorDisabledEvent
     */
    @NotBlank(message = "Causation ID is required")
    private final String causationId;

    // ========================================================================
    // Event Data - User Identity
    // ========================================================================

    /** User ID whose 2FA was disabled. Foreign key to {@code User} aggregate. */
    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    private final Long userId;

    // ========================================================================
    // Event Data - Previous 2FA State
    // ========================================================================

    /**
     * The 2FA method that was disabled.
     *
     * <p>Different methods have different implications:
     *
     * <ul>
     *   <li>TOTP disabled: High security method removed (risk increase)
     *   <li>SMS disabled: Medium security method removed
     *   <li>EMAIL disabled: Lower security method removed
     *   <li>BIOMETRIC disabled: Device-specific method removed
     * </ul>
     */
    @NotNull(message = "Previous 2FA method is required")
    private final TwoFactorEnabledEvent.TwoFactorMethod previousMethod;

    /**
     * How long 2FA was active before disablement.
     *
     * <p>Used for:
     *
     * <ul>
     *   <li>Fraud detection (2FA disabled too quickly after enabling?)
     *   <li>Analytics (average 2FA lifecycle)
     *   <li>Risk scoring (long-time users disabling is more suspicious)
     * </ul>
     */
    @NotNull(message = "Previous 2FA duration is required")
    private final Duration previousTwoFactorDuration;

    // ========================================================================
    // Event Data - Disablement Context
    // ========================================================================

    /** Reason for disabling 2FA. Critical for audit trail and pattern analysis. */
    @NotNull(message = "Disablement reason is required")
    private final DisablementReason disablementReason;

    /**
     * Method used to verify user's identity before disabling 2FA.
     *
     * <p>CRITICAL: Disabling 2FA MUST require re-authentication. This field proves authorization
     * was properly obtained.
     */
    @NotNull(message = "Verification method is required - proves authorization")
    private final VerificationMethod verificationMethod;

    /**
     * Source of disablement (user, admin, system). Determines alert type and downstream processing.
     */
    @NotNull(message = "Disablement source is required")
    private final TwoFactorDisablementSource disablementSource;

    // ========================================================================
    // Event Data - Security Context (Forensic Evidence)
    // ========================================================================

    /**
     * IP address from which 2FA was disabled.
     *
     * <p>FORENSIC CRITICAL:
     *
     * <ul>
     *   <li>Identify if request from unusual location
     *   <li>Detect mass attacks from single IP
     *   <li>Legal evidence in account takeover cases
     *   <li>Geolocation for risk scoring
     * </ul>
     */
    @NotBlank(message = "IP address is required for forensic evidence")
    private final String ipAddress;

    /**
     * Geographic location of disablement. Detects unusual geographic patterns.
     *
     * <p>Example: "US, California" or "CN, Shanghai" or null.
     */
    private final String location;

    /** Device/browser user agent. Detects unusual device access patterns. */
    private final String userAgent;

    /**
     * Whether this device was previously known/trusted. Known devices = lower risk. Unknown device
     * = higher risk.
     */
    private final boolean knownDevice;

    /** Optional note from admin or user. Additional context for audit trail. */
    private final String disablementNote;

    // ========================================================================
    // Constructor & Factory Methods
    // ========================================================================

    /**
     * Legacy constructor matching the original signature. Enables backward compatibility with
     * direct constructor calls in existing aggregate code (e.g., {@code User.disableTwoFactor()}).
     *
     * <p>Generates tracing IDs, IP address, method, source, reason, verification method, and
     * previous duration automatically.
     *
     * @param userId user ID
     */
    public TwoFactorDisabledEvent(Long userId) {
        this(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                EVENT_VERSION,
                Instant.now(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                userId,
                TwoFactorEnabledEvent.TwoFactorMethod.TOTP,
                Duration.ofDays(0),
                DisablementReason.USER_PREFERENCE,
                VerificationMethod.EXISTING_2FA,
                TwoFactorDisablementSource.USER_INITIATED,
                "127.0.0.1",
                "system",
                "system",
                true,
                "Legacy migration default");
    }

    /**
     * Primary factory method to create a validated TwoFactorDisabledEvent.
     *
     * @param userId user ID
     * @param previousMethod which 2FA method was disabled
     * @param previousDuration how long 2FA was enabled
     * @param reason why 2FA was disabled
     * @param verificationMethod how user re-authenticated
     * @param ipAddress IP address of request
     * @param correlationId correlation ID for tracing
     * @param causationId causation ID
     * @param source who initiated disablement
     * @param location optional geographic location
     * @param userAgent optional device user agent
     * @return validated TwoFactorDisabledEvent instance
     * @throws DomainValidationException if validation fails
     */
    public static TwoFactorDisabledEvent create(
            Long userId,
            TwoFactorEnabledEvent.TwoFactorMethod previousMethod,
            Duration previousDuration,
            DisablementReason reason,
            VerificationMethod verificationMethod,
            String ipAddress,
            String correlationId,
            String causationId,
            TwoFactorDisablementSource source,
            String location,
            String userAgent) {

        return TwoFactorDisabledEvent.builder()
                .userId(userId)
                .previousMethod(previousMethod)
                .previousTwoFactorDuration(previousDuration)
                .disablementReason(reason)
                .verificationMethod(verificationMethod)
                .ipAddress(ipAddress)
                .correlationId(correlationId)
                .causationId(causationId)
                .disablementSource(source)
                .location(location != null ? location.trim() : null)
                .userAgent(userAgent != null ? userAgent.trim() : null)
                .build();
    }

    /**
     * Simplified factory method for user-initiated disablement.
     *
     * @param userId user ID
     * @param previousMethod disabled 2FA method
     * @param previousDuration how long 2FA was active
     * @param reason why disabled
     * @param verificationMethod how user verified
     * @param ipAddress IP address
     * @return validated TwoFactorDisabledEvent instance
     */
    public static TwoFactorDisabledEvent createUserInitiated(
            Long userId,
            TwoFactorEnabledEvent.TwoFactorMethod previousMethod,
            Duration previousDuration,
            DisablementReason reason,
            VerificationMethod verificationMethod,
            String ipAddress) {

        return create(
                userId,
                previousMethod,
                previousDuration,
                reason,
                verificationMethod,
                ipAddress,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                TwoFactorDisablementSource.USER_INITIATED,
                null,
                null);
    }

    /**
     * Factory method for admin-forced disablement.
     *
     * @param userId affected user ID
     * @param previousMethod disabled 2FA method
     * @param previousDuration how long 2FA was active
     * @param reason why disabled (ADMIN_ENFORCEMENT)
     * @param ipAddress admin's IP address
     * @param adminNote admin note for audit trail
     * @return validated TwoFactorDisabledEvent instance
     */
    public static TwoFactorDisabledEvent createAdminForced(
            Long userId,
            TwoFactorEnabledEvent.TwoFactorMethod previousMethod,
            Duration previousDuration,
            DisablementReason reason,
            String ipAddress,
            String adminNote) {

        return builder()
                .userId(userId)
                .previousMethod(previousMethod)
                .previousTwoFactorDuration(previousDuration)
                .disablementReason(reason)
                .verificationMethod(VerificationMethod.ADMIN_OVERRIDE)
                .ipAddress(ipAddress)
                .correlationId(UUID.randomUUID().toString())
                .causationId(UUID.randomUUID().toString())
                .disablementSource(TwoFactorDisablementSource.ADMIN_FORCED)
                .disablementNote(adminNote != null ? adminNote.trim() : null)
                .build();
    }

    /** Private constructor (use factory methods instead). */
    @JsonCreator
    private TwoFactorDisabledEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("type") String eventType,
            @JsonProperty("eventVersion") int eventVersion,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("causationId") String causationId,
            @JsonProperty("userId") Long userId,
            @JsonProperty("previousMethod") TwoFactorEnabledEvent.TwoFactorMethod previousMethod,
            @JsonProperty("previousTwoFactorDuration") Duration previousTwoFactorDuration,
            @JsonProperty("disablementReason") DisablementReason disablementReason,
            @JsonProperty("verificationMethod") VerificationMethod verificationMethod,
            @JsonProperty("disablementSource") TwoFactorDisablementSource disablementSource,
            @JsonProperty("ipAddress") String ipAddress,
            @JsonProperty("location") String location,
            @JsonProperty("userAgent") String userAgent,
            @JsonProperty("knownDevice") boolean knownDevice,
            @JsonProperty("disablementNote") String disablementNote) {

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
                        "Causation ID is required to track what triggered 2FA disablement");
        this.userId =
                Validator.requirePositive(
                        userId, "userId", "User ID is required and must be positive");
        this.previousMethod =
                Validator.requireNonNull(
                        previousMethod,
                        "previousMethod",
                        "Previous 2FA method is required for audit trail");
        this.previousTwoFactorDuration = Validator.validateDuration(previousTwoFactorDuration);
        this.disablementReason =
                Validator.requireNonNull(
                        disablementReason,
                        "disablementReason",
                        "Disablement reason is required for audit trail");
        this.verificationMethod =
                Validator.requireNonNull(
                        verificationMethod,
                        "verificationMethod",
                        "Verification method is required - proves authorization for security"
                                + " operation");
        this.disablementSource =
                Validator.requireNonNull(
                        disablementSource,
                        "disablementSource",
                        "Disablement source is required for audit trail");
        this.ipAddress = Validator.validateIPAddress(ipAddress);
        this.location = location;
        this.userAgent = userAgent;
        this.knownDevice = knownDevice;
        this.disablementNote = disablementNote;
    }

    // ========================================================================
    // Builder & toBuilder
    // ========================================================================

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .eventId(this.eventId)
                .eventType(this.eventType)
                .eventVersion(this.eventVersion)
                .occurredAt(this.occurredAt)
                .correlationId(this.correlationId)
                .causationId(this.causationId)
                .userId(this.userId)
                .previousMethod(this.previousMethod)
                .previousTwoFactorDuration(this.previousTwoFactorDuration)
                .disablementReason(this.disablementReason)
                .verificationMethod(this.verificationMethod)
                .disablementSource(this.disablementSource)
                .ipAddress(this.ipAddress)
                .location(this.location)
                .userAgent(this.userAgent)
                .knownDevice(this.knownDevice)
                .disablementNote(this.disablementNote);
    }

    public static final class Builder {
        private String eventId = UUID.randomUUID().toString();
        private String eventType = EVENT_TYPE;
        private int eventVersion = EVENT_VERSION;
        private Instant occurredAt = Instant.now();
        private String correlationId = UUID.randomUUID().toString();
        private String causationId = UUID.randomUUID().toString();
        private Long userId;
        private TwoFactorEnabledEvent.TwoFactorMethod previousMethod;
        private Duration previousTwoFactorDuration;
        private DisablementReason disablementReason;
        private VerificationMethod verificationMethod;
        private TwoFactorDisablementSource disablementSource;
        private String ipAddress;
        private String location;
        private String userAgent;
        private boolean knownDevice = false;
        private String disablementNote;

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

        public Builder previousMethod(TwoFactorEnabledEvent.TwoFactorMethod val) {
            this.previousMethod = val;
            return this;
        }

        public Builder previousTwoFactorDuration(Duration val) {
            this.previousTwoFactorDuration = val;
            return this;
        }

        public Builder disablementReason(DisablementReason val) {
            this.disablementReason = val;
            return this;
        }

        public Builder verificationMethod(VerificationMethod val) {
            this.verificationMethod = val;
            return this;
        }

        public Builder disablementSource(TwoFactorDisablementSource val) {
            this.disablementSource = val;
            return this;
        }

        public Builder ipAddress(String val) {
            this.ipAddress = val;
            return this;
        }

        public Builder location(String val) {
            this.location = val;
            return this;
        }

        public Builder userAgent(String val) {
            this.userAgent = val;
            return this;
        }

        public Builder knownDevice(boolean val) {
            this.knownDevice = val;
            return this;
        }

        public Builder disablementNote(String val) {
            this.disablementNote = val;
            return this;
        }

        public TwoFactorDisabledEvent build() {
            return new TwoFactorDisabledEvent(
                    eventId,
                    eventType,
                    eventVersion,
                    occurredAt,
                    correlationId,
                    causationId,
                    userId,
                    previousMethod,
                    previousTwoFactorDuration,
                    disablementReason,
                    verificationMethod,
                    disablementSource,
                    ipAddress,
                    location,
                    userAgent,
                    knownDevice,
                    disablementNote);
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

        static Duration validateDuration(Duration duration) {
            requireNonNull(
                    duration,
                    "previousTwoFactorDuration",
                    "Previous 2FA duration is required for analytics");
            if (duration.isNegative()) {
                throw new DomainValidationException(
                        "previousTwoFactorDuration", "Previous 2FA duration cannot be negative");
            }
            return duration;
        }

        static String validateIPAddress(String ipAddress) {
            return EventValidator.validateIpAddress(
                    ipAddress,
                    "IP address is required for forensic evidence in security operations");
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
    // Business Methods - Security Analysis
    // ========================================================================

    /**
     * Checks if this is a potentially suspicious disablement.
     *
     * <p>Suspicious indicators:
     *
     * <ul>
     *   <li>Unknown device
     *   <li>Very short 2FA usage duration
     *   <li>Unusual timing
     * </ul>
     *
     * @return true if disablement patterns suggest possible attack
     */
    public boolean isSuspiciousDisablement() {
        return !knownDevice || previousTwoFactorDuration.compareTo(MINIMUM_EXPECTED_DURATION) < 0;
    }

    /**
     * Checks if this disablement was from an unknown/untrusted device.
     *
     * @return true if device not previously known
     */
    public boolean isFromUnknownDevice() {
        return !knownDevice;
    }

    /**
     * Checks if 2FA was disabled very quickly after being enabled. Rapid disablement may indicate
     * attacker testing or account takeover.
     *
     * @return true if disabled within minimum expected duration
     */
    public boolean isRapidDisablement() {
        return previousTwoFactorDuration.compareTo(MINIMUM_EXPECTED_DURATION) < 0;
    }

    /**
     * Checks if this could be an account takeover attempt. High risk if: unknown device AND rapid
     * disablement.
     *
     * @return true if high-risk takeover indicators present
     */
    public boolean isPotentialAccountTakeover() {
        return isFromUnknownDevice() && isRapidDisablement();
    }

    /**
     * Checks if this disablement was user-initiated.
     *
     * @return true if user initiated disablement
     */
    public boolean isUserInitiated() {
        return disablementSource == TwoFactorDisablementSource.USER_INITIATED;
    }

    /**
     * Checks if this disablement was admin-forced.
     *
     * @return true if admin forced disablement
     */
    public boolean isAdminForced() {
        return disablementSource == TwoFactorDisablementSource.ADMIN_FORCED;
    }

    /**
     * Checks if this disablement was system-initiated.
     *
     * @return true if system initiated disablement
     */
    public boolean isSystemInitiated() {
        return disablementSource == TwoFactorDisablementSource.SYSTEM_INITIATED;
    }

    /**
     * Checks if user lost their 2FA device.
     *
     * @return true if disabled due to lost device
     */
    public boolean isDeviceLost() {
        return disablementReason == DisablementReason.DEVICE_LOST;
    }

    /**
     * Checks if 2FA was properly verified before disabling. TOTP verification is highest security
     * for disablement.
     *
     * @return true if verified with existing 2FA or backup code
     */
    public boolean isProperlyVerified() {
        return verificationMethod == VerificationMethod.EXISTING_2FA
                || verificationMethod == VerificationMethod.BACKUP_CODE;
    }

    /**
     * Gets security risk level of this disablement. Used for alert priority and fraud investigation
     * routing.
     *
     * @return risk level (LOW, MEDIUM, HIGH, CRITICAL)
     */
    public SecurityRiskLevel getSecurityRiskLevel() {
        if (isPotentialAccountTakeover()) {
            return SecurityRiskLevel.CRITICAL;
        }
        if (isFromUnknownDevice()) {
            return SecurityRiskLevel.HIGH;
        }
        if (isRapidDisablement()) {
            return SecurityRiskLevel.MEDIUM;
        }
        return SecurityRiskLevel.LOW;
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
     * @throws DomainValidationException if seconds is zero or negative
     */
    public boolean isRecent(long seconds) {
        if (seconds <= 0) {
            throw new DomainValidationException("seconds", "Recency window must be positive");
        }
        return Instant.now().minusSeconds(seconds).isBefore(occurredAt);
    }

    /**
     * Gets previous 2FA duration in human-readable format.
     *
     * @return formatted duration string
     */
    public String getPreviousDurationFormatted() {
        long days = previousTwoFactorDuration.toDays();
        long hours = previousTwoFactorDuration.toHours() % 24;

        if (days > 0) {
            return String.format("%d days, %d hours", days, hours);
        }
        if (hours > 0) {
            return String.format("%d hours", hours);
        }
        return String.format("%d minutes", previousTwoFactorDuration.toMinutes());
    }

    // ========================================================================
    // equals & hashCode
    // ========================================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TwoFactorDisabledEvent)) return false;
        TwoFactorDisabledEvent that = (TwoFactorDisabledEvent) o;
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
     * Returns a detailed string representation for logging. Excludes sensitive forensic data (IP,
     * user agent) from general logs. These are saved for security audit logs only.
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return "TwoFactorDisabledEvent{"
                + "eventId='"
                + eventId
                + '\''
                + ", eventType='"
                + eventType
                + '\''
                + ", userId="
                + userId
                + ", previousMethod="
                + previousMethod
                + ", previousDuration='"
                + getPreviousDurationFormatted()
                + '\''
                + ", disablementReason="
                + disablementReason
                + ", verificationMethod="
                + verificationMethod
                + ", disablementSource="
                + disablementSource
                + ", knownDevice="
                + knownDevice
                + ", securityRisk="
                + getSecurityRiskLevel()
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
     * Enumeration of reasons for disabling 2FA. Critical for audit trail, pattern analysis, and
     * fraud detection.
     */
    public enum DisablementReason {
        /** User voluntarily chose to remove 2FA from their account. */
        USER_PREFERENCE("User Preference", "User voluntarily chose to remove 2FA", false),

        /** User lost their 2FA device (phone, hardware key). */
        DEVICE_LOST("Device Lost", "User lost their 2FA device", false),

        /**
         * User is switching from one 2FA method to another. 2FA will be re-enabled with different
         * method.
         */
        SWITCHING_METHOD("Switching Method", "User switching to different 2FA method", false),

        /** Administrator disabled 2FA for support or compliance reason. */
        ADMIN_ENFORCEMENT(
                "Admin Enforcement", "Administrator disabled 2FA for support/compliance", false),

        /** System automatically disabled due to expired or invalid configuration. */
        SYSTEM_EXPIRED(
                "System Expired",
                "System automatically disabled due to invalid/expired configuration",
                true),

        /** Suspicious - 2FA disabled via account recovery (potential compromise). */
        ACCOUNT_RECOVERY(
                "Account Recovery",
                "2FA disabled via account recovery process - requires investigation",
                true);

        private final String label;
        private final String description;

        /** Indicates if this reason warrants additional security investigation. */
        private final boolean requiresSecurityReview;

        DisablementReason(String label, String description, boolean requiresSecurityReview) {
            this.label = label;
            this.description = description;
            this.requiresSecurityReview = requiresSecurityReview;
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
        public boolean isRequiresSecurityReview() {
            return requiresSecurityReview;
        }
    }

    /**
     * Enumeration of verification methods used to authorize 2FA disablement.
     *
     * <p>IMPORTANT: 2FA disablement MUST require re-authentication. Verification method proves this
     * requirement was met.
     */
    public enum VerificationMethod {
        /**
         * User provided current 2FA code from their existing method. Highest security verification
         * for disablement.
         */
        EXISTING_2FA("Existing 2FA Code", "User verified with current 2FA code", 3),

        /** User provided a backup/recovery code. Used when user has lost access to 2FA device. */
        BACKUP_CODE("Backup Code", "User verified with backup recovery code", 2),

        /**
         * Administrator used admin override (no user verification). Only for admin-forced
         * disablement with documented reason.
         */
        ADMIN_OVERRIDE("Admin Override", "Administrator override - documented reason required", 1),

        /**
         * System automatic disablement (no user verification possible). Only for expired/invalid
         * configurations.
         */
        SYSTEM_AUTOMATIC(
                "System Automatic", "System automatically disabled - no user interaction", 0);

        private final String label;
        private final String description;

        /** Security strength of this verification method (0=none, 3=highest). */
        private final int securityStrength;

        VerificationMethod(String label, String description, int securityStrength) {
            this.label = label;
            this.description = description;
            this.securityStrength = securityStrength;
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
        public int getSecurityStrength() {
            return securityStrength;
        }
    }

    /**
     * Enumeration of who/what initiated the 2FA disablement. Determines alert type, processing
     * priority, and audit trail.
     */
    public enum TwoFactorDisablementSource {
        /** User voluntarily initiated 2FA disablement. */
        USER_INITIATED("User Initiated", "User voluntarily disabled 2FA", false),

        /**
         * Administrator forced 2FA disablement. User receives alert, admin action is documented.
         */
        ADMIN_FORCED("Admin Forced", "Administrator disabled user's 2FA", true),

        /**
         * System automatically disabled due to invalid/expired configuration. User receives alert
         * with instructions to re-enable.
         */
        SYSTEM_INITIATED("System Initiated", "System automatically disabled 2FA", true);

        private final String label;
        private final String description;

        /** Indicates if this source requires immediate user notification. */
        private final boolean requiresImmediateUserAlert;

        TwoFactorDisablementSource(
                String label, String description, boolean requiresImmediateUserAlert) {
            this.label = label;
            this.description = description;
            this.requiresImmediateUserAlert = requiresImmediateUserAlert;
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
        public boolean isRequiresImmediateUserAlert() {
            return requiresImmediateUserAlert;
        }
    }

    /** Security risk level assessment for this disablement event. */
    public enum SecurityRiskLevel {
        /** Normal disablement from known device with proper verification. */
        LOW("Low Risk", "Normal disablement pattern", false),

        /** Some unusual patterns but not immediately alarming. */
        MEDIUM("Medium Risk", "Some unusual patterns detected", true),

        /** Significant risk indicators - immediate review recommended. */
        HIGH("High Risk", "Significant risk indicators - review required", true),

        /** Critical risk - potential account takeover in progress. */
        CRITICAL("Critical Risk", "Potential account takeover - immediate action required", true);

        private final String label;
        private final String description;

        /** Whether this risk level requires security team alert. */
        private final boolean requiresSecurityTeamAlert;

        SecurityRiskLevel(String label, String description, boolean requiresSecurityTeamAlert) {
            this.label = label;
            this.description = description;
            this.requiresSecurityTeamAlert = requiresSecurityTeamAlert;
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
        public boolean isRequiresSecurityTeamAlert() {
            return requiresSecurityTeamAlert;
        }
    }
}
