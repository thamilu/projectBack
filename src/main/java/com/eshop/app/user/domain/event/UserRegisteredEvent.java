package com.eshop.app.user.domain.event;

import com.eshop.app.core.events.contract.DomainEvent;
import com.eshop.app.core.exception.business.DomainValidationException;
import com.eshop.app.user.shared.domain.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Immutable domain event published when a new user successfully registers.
 *
 * <p><b>Business Meaning:</b> Signals that a new user account has been persisted and Keycloak has
 * been synchronized. Downstream services react to this event to complete the onboarding flow (email
 * verification, profile creation, permissions, analytics).
 *
 * <p><b>Event Guarantees:</b>
 *
 * <ul>
 *   <li>Published ONLY after user successfully persisted in database
 *   <li>Keycloak account created and synchronized before publishing
 *   <li>All fields validated at construction — no partially valid instances
 *   <li>Immutable after creation — no setters, all fields final
 *   <li>Timestamp is absolute UTC — safe for distributed ordering
 *   <li>EventId is unique UUID — enables idempotent processing
 *   <li>Email and keycloakId excluded from toString() — prevents PII in logs
 * </ul>
 *
 * <p><b>Creation Contract:</b>
 *
 * <ul>
 *   <li>Use {@link #create} for full context (production default)
 *   <li>Use {@link #createSimple} for standard registration without campaign
 *   <li>Use {@link #createForSystem} for programmatic/migration registrations
 *   <li>Use {@link #builder()} for test scenarios requiring custom infrastructure fields
 *   <li>Direct constructor calls are not permitted — constructor is private
 * </ul>
 *
 * <p><b>Registration Flow:</b>
 *
 * <pre>
 * User submits form → Validate → Create Keycloak account → Persist User
 *   → UserRegisteredEvent [THIS]
 *       ├─ EmailService        → send verification email
 *       ├─ UserProfileService  → create default profile
 *       ├─ PermissionService   → assign initial permissions
 *       ├─ AnalyticsService    → track registration funnel
 *       ├─ NotificationService → create notification preferences
 *       └─ AuditService        → compliance log
 *   → EmailVerifiedEvent
 *   → UserActivatedEvent
 * </pre>
 *
 * <p><b>Serialization Contract:</b>
 *
 * <ul>
 *   <li>Jackson deserializes via {@link JsonCreator} constructor
 *   <li>All fields serialized with explicit {@link JsonProperty} names
 *   <li>Computed boolean methods annotated {@link JsonIgnore} — not serialized
 *   <li>{@code eventType} serialized as {@code "eventType"} — consistent with field name
 * </ul>
 *
 * <p><b>Privacy &amp; Security:</b>
 *
 * <ul>
 *   <li>{@code email} — excluded from toString() and all error messages
 *   <li>{@code keycloakId} — excluded from toString() and all error messages
 * </ul>
 *
 * @see DomainEvent
 * @version 1.0
 * @since 1.0
 */
public final class UserRegisteredEvent implements DomainEvent {

    // =========================================================================
    // Constants
    // =========================================================================

    /**
     * Event type identifier — used for event routing and polymorphic handling. Stable across schema
     * versions to avoid consumer breakage.
     */
    public static final String EVENT_TYPE = "user.registered";

    /**
     * Current event schema version. Increment on breaking structural changes; add migration
     * handler.
     */
    public static final int EVENT_VERSION = 1;

    /** Registration source: web browser. */
    public static final String SOURCE_WEB = "web";

    /** Registration source: mobile application (iOS or Android). */
    public static final String SOURCE_MOBILE = "mobile";

    /** Registration source: direct API call (programmatic or partner integration). */
    public static final String SOURCE_API = "api";

    /** Registration source: invitation or referral flow. */
    public static final String SOURCE_INVITED = "invited";

    /** Registration source: internal system process (migration, admin creation). */
    public static final String SOURCE_SYSTEM = "system";

    /** RFC 5321 maximum email address length. */
    private static final int MAX_EMAIL_LENGTH = 254;

    /**
     * UUID pattern for Keycloak ID validation (case-insensitive). Format:
     * xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
     */
    private static final Pattern KEYCLOAK_ID_PATTERN =
            Pattern.compile(
                    "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
                    Pattern.CASE_INSENSITIVE);

    /**
     * RFC 5322 simplified email pattern. Validates standard email formats. Quoted strings and IP
     * literals are rejected at the application boundary before reaching domain layer.
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // =========================================================================
    // Event Identity & Infrastructure Metadata
    // =========================================================================

    /**
     * Unique event identifier (UUID v4). Enables idempotent processing — consumers detect duplicate
     * deliveries and skip already-processed events using this ID.
     */
    @JsonProperty("eventId")
    private final String eventId;

    /**
     * Event type identifier — always {@value EVENT_TYPE} for this class. Stored in the event for
     * schema-on-read deserialization support.
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
     * Absolute UTC timestamp when event occurred. Used for event ordering, time-based projections,
     * and audit trails.
     */
    @JsonProperty("occurredAt")
    private final Instant occurredAt;

    // =========================================================================
    // Distributed Tracing Identifiers
    // =========================================================================

    /**
     * Correlation ID linking all events within a single user-facing flow. Propagated from the
     * inbound request (MDC/header) or generated fresh.
     */
    @JsonProperty("correlationId")
    private final String correlationId;

    /**
     * Causation ID identifying which command or event triggered this event. Example:
     * RegisterUserCommand.commandId → this event's causationId.
     */
    @JsonProperty("causationId")
    private final String causationId;

    // =========================================================================
    // User Identity Data
    // =========================================================================

    /**
     * Persisted user ID (primary key of User aggregate). Must be positive — set after database
     * persistence.
     */
    @JsonProperty("userId")
    private final Long userId;

    /**
     * Normalized email address (trimmed + lowercase).
     *
     * <p><b>Privacy:</b> Excluded from {@link #toString()} and all error messages.
     */
    @JsonProperty("email")
    private final String email;

    /**
     * Keycloak external identity provider user ID (UUID format).
     *
     * <p><b>Security:</b> Excluded from {@link #toString()} and all error messages.
     */
    @JsonProperty("keycloakId")
    private final String keycloakId;

    /**
     * Role assigned to the user at registration time. Typically {@link UserRole#CUSTOMER} for
     * self-registration.
     */
    @JsonProperty("initialRole")
    private final UserRole initialRole;

    // =========================================================================
    // Registration Context
    // =========================================================================

    /**
     * Channel through which registration occurred (normalized to lowercase). Valid values: {@link
     * #SOURCE_WEB}, {@link #SOURCE_MOBILE}, {@link #SOURCE_API}, {@link #SOURCE_INVITED}, {@link
     * #SOURCE_SYSTEM}.
     */
    @JsonProperty("registrationSource")
    private final String registrationSource;

    /**
     * Origin domain, URL, or application identifier where registration was submitted. Examples:
     * {@code "https://eshop.com"}, {@code "com.eshop.android"}.
     */
    @JsonProperty("registrationOrigin")
    private final String registrationOrigin;

    /**
     * Optional marketing campaign attribution ID. Null when no campaign is attributed. Trimmed when
     * present.
     */
    @JsonProperty("campaignId")
    private final String campaignId;

    // =========================================================================
    // Single Private Constructor — Validation Exactly Once
    // =========================================================================

    /**
     * Single validated constructor. All field assignment and validation occurs here.
     *
     * <p>Annotated with {@link JsonCreator} for Jackson deserialization support. Validation is
     * strict — no silent fallbacks for any field. Infrastructure fields and domain fields are all
     * validated.
     *
     * @throws DomainValidationException if any field fails validation
     */
    @JsonCreator
    private UserRegisteredEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("eventVersion") int eventVersion,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("causationId") String causationId,
            @JsonProperty("userId") Long userId,
            @JsonProperty("email") String email,
            @JsonProperty("keycloakId") String keycloakId,
            @JsonProperty("initialRole") UserRole initialRole,
            @JsonProperty("registrationSource") String registrationSource,
            @JsonProperty("registrationOrigin") String registrationOrigin,
            @JsonProperty("campaignId") String campaignId) {

        // Infrastructure fields — strict, no silent fallbacks
        this.eventId = Validator.requireNonBlank(eventId, "eventId", "Event ID is required");
        this.eventType =
                Validator.requireNonBlank(eventType, "eventType", "Event type is required");
        this.eventVersion =
                Validator.requirePositiveInt(
                        eventVersion, "eventVersion", "Event version must be positive");
        this.occurredAt =
                Validator.requireNonNull(occurredAt, "occurredAt", "Event timestamp is required");

        // Tracing identifiers
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

        // Domain fields — full validation + normalization via Validator (DRY)
        this.userId =
                Validator.requirePositiveLong(
                        userId, "userId", "User ID is required and must be positive");
        this.email = Validator.validateAndNormalizeEmail(email);
        this.keycloakId = Validator.validateAndNormalizeKeycloakId(keycloakId);
        this.initialRole =
                Validator.requireNonNull(initialRole, "initialRole", "Initial role is required");
        this.registrationSource = Validator.validateAndNormalizeSource(registrationSource);
        this.registrationOrigin = Validator.validateAndNormalizeOrigin(registrationOrigin);
        this.campaignId = Validator.normalizeCampaignId(campaignId);
    }

    // =========================================================================
    // Factory Methods — Intended Public Creation API
    // =========================================================================

    /**
     * Creates a fully contextualized event for production use.
     *
     * <p>Auto-generates: {@code eventId}, {@code eventType}, {@code eventVersion}, {@code
     * occurredAt}, {@code correlationId}, {@code causationId}.
     *
     * @param userId persisted user ID — must be positive
     * @param email user email — validated and normalized
     * @param keycloakId Keycloak UUID — must be valid UUID format
     * @param initialRole role at registration — must be non-null
     * @param registrationSource registration channel — must be non-blank
     * @param registrationOrigin origin domain or app — must be non-blank
     * @param campaignId optional campaign attribution ID — nullable
     * @return validated, immutable event
     * @throws DomainValidationException if any required field is invalid
     */
    public static UserRegisteredEvent create(
            Long userId,
            String email,
            String keycloakId,
            UserRole initialRole,
            String registrationSource,
            String registrationOrigin,
            String campaignId) {

        return builder()
                .userId(userId)
                .email(email)
                .keycloakId(keycloakId)
                .initialRole(initialRole)
                .registrationSource(registrationSource)
                .registrationOrigin(registrationOrigin)
                .campaignId(campaignId)
                .build();
    }

    /**
     * Creates an event without campaign attribution.
     *
     * <p>Convenience method for standard registrations with no marketing campaign. Delegates to
     * {@link #create(Long, String, String, UserRole, String, String, String)}.
     *
     * @param userId persisted user ID
     * @param email user email
     * @param keycloakId Keycloak UUID
     * @param initialRole role at registration
     * @param registrationSource registration channel
     * @param registrationOrigin origin domain or app
     * @return validated, immutable event with null campaignId
     * @throws DomainValidationException if any required field is invalid
     */
    public static UserRegisteredEvent createSimple(
            Long userId,
            String email,
            String keycloakId,
            UserRole initialRole,
            String registrationSource,
            String registrationOrigin) {

        return create(
                userId,
                email,
                keycloakId,
                initialRole,
                registrationSource,
                registrationOrigin,
                null);
    }

    /**
     * Creates an event for system-originated registrations (migrations, admin creation).
     *
     * <p>Sets {@code registrationSource} and {@code registrationOrigin} to {@link #SOURCE_SYSTEM}
     * automatically. Replaces the old public legacy constructor.
     *
     * @param userId persisted user ID
     * @param email user email
     * @param keycloakId Keycloak UUID
     * @param initialRole role at registration
     * @return validated, immutable event with system source and origin
     * @throws DomainValidationException if any required field is invalid
     */
    public static UserRegisteredEvent createForSystem(
            Long userId, String email, String keycloakId, UserRole initialRole) {

        return create(userId, email, keycloakId, initialRole, SOURCE_SYSTEM, SOURCE_SYSTEM, null);
    }

    // =========================================================================
    // Builder
    // =========================================================================

    /**
     * Returns a new {@link Builder} for constructing a {@code UserRegisteredEvent}.
     *
     * <p>Infrastructure fields default to auto-generated values. Domain fields must be explicitly
     * set — no domain-level defaults.
     *
     * @return new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a pre-populated {@link Builder} seeded from this event.
     *
     * <p>Used for creating modified copies while retaining original values. Useful in event
     * sourcing replay and test setup scenarios.
     *
     * @return builder with all fields copied from this event
     */
    public Builder toBuilder() {
        return new Builder()
                .eventId(this.eventId)
                .occurredAt(this.occurredAt)
                .correlationId(this.correlationId)
                .causationId(this.causationId)
                .userId(this.userId)
                .email(this.email)
                .keycloakId(this.keycloakId)
                .initialRole(this.initialRole)
                .registrationSource(this.registrationSource)
                .registrationOrigin(this.registrationOrigin)
                .campaignId(this.campaignId);
        // eventType and eventVersion intentionally omitted:
        // they are type-level constants fixed in build().
        // Overriding them would corrupt event routing and schema handling.
    }

    /**
     * Fluent builder for {@link UserRegisteredEvent}.
     *
     * <p><b>Infrastructure defaults (auto-generated, overridable for replay/testing):</b>
     *
     * <ul>
     *   <li>{@code eventId} — UUID v4
     *   <li>{@code occurredAt} — {@code Instant.now()} at builder creation
     *   <li>{@code correlationId} — UUID v4
     *   <li>{@code causationId} — UUID v4
     * </ul>
     *
     * <p><b>Fixed values (not settable — type-level constants, hardcoded in {@link #build()}):</b>
     *
     * <ul>
     *   <li>{@code eventType} — always {@link UserRegisteredEvent#EVENT_TYPE}
     *   <li>{@code eventVersion} — always {@link UserRegisteredEvent#EVENT_VERSION}
     * </ul>
     *
     * <p><b>Domain fields (no defaults — must be explicitly set):</b> {@code userId}, {@code
     * email}, {@code keycloakId}, {@code initialRole}, {@code registrationSource}, {@code
     * registrationOrigin}.
     *
     * <p>Validation occurs in the constructor — not in the builder. This ensures a single
     * validation path regardless of creation route.
     */
    public static final class Builder {

        // Infrastructure — overridable for replay/testing scenarios
        private String eventId = UUID.randomUUID().toString();
        private Instant occurredAt = Instant.now();
        private String correlationId = UUID.randomUUID().toString();
        private String causationId = UUID.randomUUID().toString();

        // Domain — no defaults, must be explicitly set
        private Long userId;
        private String email;
        private String keycloakId;
        private UserRole initialRole;
        private String registrationSource;
        private String registrationOrigin;
        private String campaignId;

        private Builder() {}

        /** Overrides auto-generated event ID. Use only for replay or testing. */
        public Builder eventId(String val) {
            this.eventId = val;
            return this;
        }

        /** Overrides auto-generated timestamp. Use only for replay or testing. */
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

        /** Sets email — validated and normalized (trimmed + lowercase) at build time. */
        public Builder email(String val) {
            this.email = val;
            return this;
        }

        /** Sets Keycloak UUID — validated as UUID format at build time. */
        public Builder keycloakId(String val) {
            this.keycloakId = val;
            return this;
        }

        /** Sets initial role — must be non-null. */
        public Builder initialRole(UserRole val) {
            this.initialRole = val;
            return this;
        }

        /** Sets registration source — normalized to lowercase at build time. */
        public Builder registrationSource(String val) {
            this.registrationSource = val;
            return this;
        }

        /** Sets registration origin domain or app identifier. */
        public Builder registrationOrigin(String val) {
            this.registrationOrigin = val;
            return this;
        }

        /** Sets optional campaign attribution ID — nullable, trimmed if present. */
        public Builder campaignId(String val) {
            this.campaignId = val;
            return this;
        }

        /**
         * Constructs and validates the {@link UserRegisteredEvent}.
         *
         * <p>All validation is delegated to the constructor. {@code eventType} and {@code
         * eventVersion} are passed as constants — they cannot be overridden by the builder.
         *
         * @return validated, immutable event
         * @throws DomainValidationException if any required field is missing or invalid
         */
        public UserRegisteredEvent build() {
            return new UserRegisteredEvent(
                    eventId,
                    EVENT_TYPE, // fixed — builder cannot override event type
                    EVENT_VERSION, // fixed — builder cannot override schema version
                    occurredAt,
                    correlationId,
                    causationId,
                    userId,
                    email,
                    keycloakId,
                    initialRole,
                    registrationSource,
                    registrationOrigin,
                    campaignId);
        }
    }

    // =========================================================================
    // Validator — DRY, Stateless, Single Responsibility
    // =========================================================================

    /**
     * Stateless field validators for {@link UserRegisteredEvent}.
     *
     * <p>Responsibilities:
     *
     * <ul>
     *   <li>Each validation rule defined exactly once (DRY)
     *   <li>Validation logic isolated from event state (Single Responsibility)
     *   <li>Package-private for independent unit testing
     *   <li>Reusable across related events in same package
     * </ul>
     *
     * <p><b>Privacy contract:</b> No field values included in exception messages. Error messages
     * describe the rule, not the rejected value.
     */
    static final class Validator {

        private Validator() {
            throw new UnsupportedOperationException("Utility class — do not instantiate");
        }

        /**
         * Requires non-null, non-blank string. Returns trimmed value.
         *
         * @throws DomainValidationException if null or blank
         */
        static String requireNonBlank(String value, String field, String message) {
            return EventValidator.requireNonBlank(value, field, message);
        }

        /**
         * Requires non-null value.
         *
         * @throws DomainValidationException if null
         */
        static <T> T requireNonNull(T value, String field, String message) {
            return EventValidator.requireNonNull(value, field, message);
        }

        /**
         * Requires positive Long (non-null and greater than zero).
         *
         * @throws DomainValidationException if null or non-positive
         */
        static Long requirePositiveLong(Long value, String field, String message) {
            return EventValidator.requirePositiveLong(value, field, message);
        }

        /**
         * Requires positive int (greater than zero).
         *
         * @throws DomainValidationException if non-positive
         */
        static int requirePositiveInt(int value, String field, String message) {
            return EventValidator.requirePositiveInt(value, field, message);
        }

        /**
         * Validates and normalizes email address.
         *
         * <p>Normalization: trim whitespace, convert to lowercase. Validation order: not blank →
         * length ≤ 254 → RFC 5322 format.
         *
         * <p><b>Privacy:</b> Email value excluded from all error messages.
         *
         * @return normalized email (trimmed, lowercase)
         * @throws DomainValidationException if null, blank, too long, or invalid format
         */
        static String validateAndNormalizeEmail(String email) {
            if (email == null || email.isBlank()) {
                throw new DomainValidationException(
                        "email", "Email is required and cannot be blank");
            }

            String normalized = email.trim().toLowerCase();

            if (normalized.length() > MAX_EMAIL_LENGTH) {
                throw new DomainValidationException(
                        "email",
                        "Email exceeds maximum length of "
                                + MAX_EMAIL_LENGTH
                                + " characters (RFC 5321)");
            }

            if (!EMAIL_PATTERN.matcher(normalized).matches()) {
                throw new DomainValidationException(
                        "email", "Email format is invalid — must match RFC 5322 pattern");
            }

            return normalized;
        }

        /**
         * Validates and normalizes Keycloak user ID.
         *
         * <p>Normalization: trim whitespace. Validation: UUID format
         * (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx).
         *
         * <p><b>Security:</b> Keycloak ID value excluded from all error messages.
         *
         * @return trimmed keycloakId
         * @throws DomainValidationException if null, blank, or not valid UUID format
         */
        static String validateAndNormalizeKeycloakId(String keycloakId) {
            if (keycloakId == null || keycloakId.isBlank()) {
                throw new DomainValidationException("keycloakId", "Keycloak ID is required");
            }

            String trimmed = keycloakId.trim();

            if (!KEYCLOAK_ID_PATTERN.matcher(trimmed).matches()) {
                throw new DomainValidationException(
                        "keycloakId",
                        "Keycloak ID must be a valid UUID "
                                + "(format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)");
            }

            return trimmed;
        }

        /**
         * Validates and normalizes registration source.
         *
         * <p>Normalization: trim whitespace, convert to lowercase. After normalization, direct
         * {@link String#equals} comparisons with {@code SOURCE_*} constants are safe — no need for
         * {@code equalsIgnoreCase}.
         *
         * @return normalized source (trimmed, lowercase)
         * @throws DomainValidationException if null or blank
         */
        static String validateAndNormalizeSource(String source) {
            if (source == null || source.isBlank()) {
                throw new DomainValidationException(
                        "registrationSource",
                        "Registration source is required "
                                + "(expected: web, mobile, api, invited, or system)");
            }
            return source.trim().toLowerCase();
        }

        /**
         * Validates and normalizes registration origin.
         *
         * <p>Normalization: trim whitespace only. Original casing is preserved — URLs and app
         * identifiers are case-sensitive.
         *
         * @return trimmed origin
         * @throws DomainValidationException if null or blank
         */
        static String validateAndNormalizeOrigin(String origin) {
            if (origin == null || origin.isBlank()) {
                throw new DomainValidationException(
                        "registrationOrigin",
                        "Registration origin is required "
                                + "(expected: domain URL or application identifier)");
            }
            return origin.trim();
        }

        /**
         * Normalizes optional campaign ID.
         *
         * <p>Returns null for null or blank input (field is optional). Returns trimmed value for
         * non-blank input.
         *
         * @param campaignId candidate campaign ID (nullable)
         * @return trimmed campaignId, or null if absent or blank
         */
        static String normalizeCampaignId(String campaignId) {
            if (campaignId == null || campaignId.isBlank()) {
                return null;
            }
            return campaignId.trim();
        }
    }

    // =========================================================================
    // DomainEvent Interface Implementation
    // =========================================================================

    /** {@inheritDoc} Returns {@link #EVENT_TYPE}. */
    @Override
    public String getEventType() {
        return eventType;
    }

    /** {@inheritDoc} Returns epoch milliseconds of {@link #getOccurredAtInstant()}. */
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
     * Returns event timestamp as {@link Instant} for time API compatibility.
     *
     * <p>Use this over {@link #getOccurredAt()} when working with {@link java.time} APIs or needing
     * sub-millisecond precision.
     *
     * @return event timestamp (UTC)
     */
    @JsonIgnore
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
     * @return persisted user ID of the newly registered user
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Returns the normalized email address (trimmed + lowercase).
     *
     * <p><b>Privacy:</b> Do not log or include in error messages.
     *
     * @return normalized email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the Keycloak external identity provider user ID.
     *
     * <p><b>Security:</b> Do not log or include in error messages.
     *
     * @return Keycloak UUID
     */
    public String getKeycloakId() {
        return keycloakId;
    }

    /**
     * @return role assigned at registration
     */
    public UserRole getInitialRole() {
        return initialRole;
    }

    /**
     * @return registration channel (normalized lowercase)
     */
    public String getRegistrationSource() {
        return registrationSource;
    }

    /**
     * @return origin domain or application identifier
     */
    public String getRegistrationOrigin() {
        return registrationOrigin;
    }

    /**
     * @return marketing campaign ID, or null if no campaign attribution
     */
    public String getCampaignId() {
        return campaignId;
    }

    // =========================================================================
    // Business Query Methods
    // =========================================================================

    /**
     * Whether registration occurred via web browser.
     *
     * <p>Safe to use {@link String#equals} (not {@code equalsIgnoreCase}) because {@code
     * registrationSource} is normalized to lowercase at construction.
     *
     * @return true if {@code registrationSource} equals {@link #SOURCE_WEB}
     */
    @JsonIgnore
    public boolean isWebRegistration() {
        return SOURCE_WEB.equals(registrationSource);
    }

    /**
     * Whether registration occurred via mobile application.
     *
     * @return true if {@code registrationSource} equals {@link #SOURCE_MOBILE}
     */
    @JsonIgnore
    public boolean isMobileRegistration() {
        return SOURCE_MOBILE.equals(registrationSource);
    }

    /**
     * Whether registration occurred via direct API call.
     *
     * @return true if {@code registrationSource} equals {@link #SOURCE_API}
     */
    @JsonIgnore
    public boolean isAPIRegistration() {
        return SOURCE_API.equals(registrationSource);
    }

    /**
     * Whether registration occurred via invitation or referral flow.
     *
     * @return true if {@code registrationSource} equals {@link #SOURCE_INVITED}
     */
    @JsonIgnore
    public boolean isInvitedRegistration() {
        return SOURCE_INVITED.equals(registrationSource);
    }

    /**
     * Whether registration is attributed to a marketing campaign.
     *
     * @return true if {@code campaignId} is non-null and non-blank
     */
    @JsonIgnore
    public boolean hasCampaignAttribution() {
        return campaignId != null && !campaignId.isBlank();
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
     * Whether user registered with {@link UserRole#CUSTOMER} role.
     *
     * @return true if {@code initialRole} is CUSTOMER
     */
    @JsonIgnore
    public boolean isCustomerRegistration() {
        return UserRole.CUSTOMER == initialRole;
    }

    /**
     * Whether user registered with {@link UserRole#SELLER} role.
     *
     * @return true if {@code initialRole} is SELLER
     */
    @JsonIgnore
    public boolean isSellerRegistration() {
        return UserRole.SELLER == initialRole;
    }

    /**
     * Whether user registered with {@link UserRole#DELIVERY_AGENT} role.
     *
     * @return true if {@code initialRole} is DELIVERY_AGENT
     */
    @JsonIgnore
    public boolean isDeliveryAgentRegistration() {
        return UserRole.DELIVERY_AGENT == initialRole;
    }

    // =========================================================================
    // Object Contract
    // =========================================================================

    /**
     * Two events are equal if and only if they have the same {@code eventId}.
     *
     * <p>EventId is UUID v4 — collision probability negligible. Enables deduplication in Sets/Maps
     * and idempotent event store processing.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserRegisteredEvent other)) {
            return false;
        }
        return Objects.equals(eventId, other.eventId);
    }

    /**
     * @return hash based solely on {@code eventId} — consistent with {@link #equals}
     */
    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    /**
     * Log-safe string representation.
     *
     * <p><b>Privacy:</b> Intentionally excludes {@code email} and {@code keycloakId} to prevent
     * PII/credential exposure in logs, traces, and error reports.
     */
    @Override
    public String toString() {
        return "UserRegisteredEvent{"
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
                + ", initialRole="
                + initialRole
                + ", registrationSource='"
                + registrationSource
                + '\''
                + ", registrationOrigin='"
                + registrationOrigin
                + '\''
                + ", hasCampaignAttribution="
                + hasCampaignAttribution()
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
}
