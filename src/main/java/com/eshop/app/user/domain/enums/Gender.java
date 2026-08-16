package com.eshop.app.user.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * Type-safe enumeration for gender identity options in user profiles.
 *
 * <p>Provides comprehensive gender options following modern inclusivity standards and GDPR
 * compliance. Each option includes:
 *
 * <ul>
 *   <li>Display-friendly label for UI rendering
 *   <li>Short code for API/database optimization
 *   <li>Description for clarity and documentation
 * </ul>
 *
 * <h3>Usage Examples:</h3>
 *
 * <pre>{@code
 * // Display in UI
 * String label = Gender.NON_BINARY.getDisplayName(); // "Non-Binary"
 *
 * // Store in database
 * String code = Gender.MALE.getCode(); // "M"
 *
 * // Parse from user input
 * Optional<Gender> gender = Gender.fromString("female"); // FEMALE
 *
 * // Parse from API code
 * Optional<Gender> gender = Gender.fromCode("NB"); // NON_BINARY
 *
 * // Get all selectable options for UI
 * List<Gender> options = Gender.getAllSelectableOptions();
 * }</pre>
 *
 * <h3>Privacy & Compliance:</h3>
 *
 * <p>Includes {@code PREFER_NOT_TO_SAY} option to ensure GDPR/privacy compliance by allowing users
 * to opt out of providing gender information.
 *
 * @author Your Team
 * @version 2.0
 * @since 1.0
 * @see <a href="https://www.hrc.org/resources/glossary-of-terms">HRC Gender Terminology</a>
 */
@Getter
public enum Gender {

    // ========================================================================
    // Enum Constants (Organized by Category)
    // ========================================================================

    /** Male gender identity. */
    MALE("M", "Male", "Identifies as male", true),

    /** Female gender identity. */
    FEMALE("F", "Female", "Identifies as female", true),

    /** Non-binary gender identity. */
    NON_BINARY("NB", "Non-Binary", "Gender identity outside traditional binary", true),

    /** Privacy option - user prefers not to disclose. */
    PREFER_NOT_TO_SAY("X", "Prefer Not to Say", "User prefers not to disclose gender", true);

    // ========================================================================
    // Static Cache for Fast Lookups (DRY Principle)
    // ========================================================================

    /** Cache for code-based lookups (e.g., "M" → MALE). */
    private static final Map<String, Gender> CODE_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toUnmodifiableMap(g -> g.getCode(), Function.identity()));

    /** Cache for case-insensitive name lookups (e.g., "male" → MALE). */
    private static final Map<String, Gender> NAME_MAP =
            Arrays.stream(values())
                    .collect(
                            Collectors.toUnmodifiableMap(
                                    g -> g.name().toLowerCase(), Function.identity()));

    /** Cache for display name lookups (e.g., "Female" → FEMALE). */
    private static final Map<String, Gender> DISPLAY_NAME_MAP =
            Arrays.stream(values())
                    .collect(
                            Collectors.toUnmodifiableMap(
                                    g -> g.getDisplayName().toLowerCase(), Function.identity()));

    // ========================================================================
    // Instance Fields
    // ========================================================================

    /** Short code for database/API usage (e.g., "M", "F", "NB"). */
    private final String code;

    /** Human-readable display name for UI (e.g., "Male", "Female", "Non-Binary"). */
    @JsonValue // Jackson will serialize using this field
    private final String displayName;

    /** Detailed description for documentation and accessibility. */
    private final String description;

    /** Indicates if this option should be shown in user-facing selections. */
    private final boolean selectable;

    // ========================================================================
    // Constructor
    // ========================================================================

    /**
     * Private constructor for enum constants.
     *
     * @param code short code for database/API
     * @param displayName human-readable name for UI
     * @param description detailed description
     * @param selectable whether this option is user-selectable
     */
    Gender(String code, String displayName, String description, boolean selectable) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
        this.selectable = selectable;
    }

    // ========================================================================
    // Parsing Methods (DRY - Reusable Across Application)
    // ========================================================================

    /**
     * Parses gender from string input (case-insensitive).
     *
     * <p>Supports multiple input formats:
     *
     * <ul>
     *   <li>Enum name: "MALE", "male", "Male"
     *   <li>Display name: "Non-Binary", "non-binary"
     *   <li>Code: "M", "F", "NB", "X"
     * </ul>
     *
     * @param value string representation of gender
     * @return Optional containing the matched Gender, or empty if not found
     */
    @JsonCreator // Jackson will use this for deserialization
    public static Optional<Gender> fromString(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        String normalized = value.trim();

        // Try exact code match first (fastest)
        Gender byCode = CODE_MAP.get(normalized.toUpperCase());
        if (byCode != null) {
            return Optional.of(byCode);
        }

        // Try enum name match
        Gender byName = NAME_MAP.get(normalized.toLowerCase());
        if (byName != null) {
            return Optional.of(byName);
        }

        // Try display name match
        Gender byDisplayName = DISPLAY_NAME_MAP.get(normalized.toLowerCase());
        if (byDisplayName != null) {
            return Optional.of(byDisplayName);
        }

        return Optional.empty();
    }

    /**
     * Parses gender from short code (e.g., "M", "F", "NB").
     *
     * @param code short code
     * @return Optional containing the matched Gender, or empty if not found
     */
    public static Optional<Gender> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(CODE_MAP.get(code.trim().toUpperCase()));
    }

    /**
     * Parses gender from string with fallback to default.
     *
     * @param value string representation
     * @param defaultGender fallback if parsing fails
     * @return parsed Gender or default
     */
    public static Gender fromStringOrDefault(String value, Gender defaultGender) {
        return fromString(value).orElse(defaultGender);
    }

    /**
     * Parses gender from string or throws exception.
     *
     * @param value string representation
     * @return parsed Gender
     * @throws IllegalArgumentException if value is invalid
     */
    public static Gender fromStringOrThrow(String value) {
        return fromString(value)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        String.format(
                                                "Invalid gender value: '%s'. Valid values: %s",
                                                value, Arrays.toString(values()))));
    }

    // ========================================================================
    // Query Methods (Business Logic Support)
    // ========================================================================

    /**
     * Gets all gender options that should be shown in UI selections.
     *
     * @return array of selectable Gender options
     */
    public static Gender[] getAllSelectableOptions() {
        return Arrays.stream(values())
                .filter(g -> g != null && g.isSelectable())
                .toArray(size -> new Gender[size]);
    }

    /**
     * Checks if this gender requires additional pronoun selection.
     *
     * <p>Non-binary identities typically require explicit pronoun selection.
     *
     * @return true if pronoun selection should be presented
     */
    public boolean requiresPronounSelection() {
        return this == NON_BINARY;
    }

    /**
     * Checks if this gender represents a binary identity.
     *
     * @return true if MALE or FEMALE
     */
    public boolean isBinary() {
        return this == MALE || this == FEMALE;
    }

    /**
     * Checks if gender information is disclosed.
     *
     * @return false if user prefers not to say, true otherwise
     */
    public boolean isDisclosed() {
        return this != PREFER_NOT_TO_SAY;
    }

    // ========================================================================
    // Display Methods (UI Support)
    // ========================================================================

    /**
     * Gets abbreviated display text (e.g., for badges, small UI elements).
     *
     * @return short display text
     */
    public String getAbbreviation() {
        return code;
    }

    /**
     * Gets formatted display with code (e.g., "Male (M)").
     *
     * @return display name with code
     */
    public String getFullDisplay() {
        return String.format("%s (%s)", displayName, code);
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
     * Validates if a string represents a valid gender.
     *
     * @param value string to validate
     * @return true if valid gender value
     */
    public static boolean isValid(String value) {
        return fromString(value).isPresent();
    }

    /**
     * Validates if a code represents a valid gender.
     *
     * @param code code to validate
     * @return true if valid gender code
     */
    public static boolean isValidCode(String code) {
        return fromCode(code).isPresent();
    }
}
