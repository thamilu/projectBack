package com.eshop.app.seller.api.request;

import com.eshop.app.seller.api.request.validation.ValidationGroups;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reusable address request DTO.
 *
 * <p>Validations are grouped under {@link ValidationGroups.Step5Address} to support multi-step
 * checkout and registration validations.
 *
 * <h3>Validation Strategy</h3>
 *
 * <ul>
 *   <li>Required fields: {@code addressLine1}, {@code city}, {@code state}, {@code pincode}, {@code
 *       country} — enforced via {@code @NotBlank}
 *   <li>Optional fields: {@code addressLine2}, {@code district}, {@code taluk}, {@code
 *       googleMapsUrl} — {@code null} is accepted; if a value is provided, it must conform to
 *       format constraints
 * </ul>
 *
 * <h3>Regex Design Notes</h3>
 *
 * <ul>
 *   <li>Address line patterns avoid lookaheads to prevent ReDoS under adversarial input
 *   <li>Optional field patterns use {@code ^(...)?$} structure — null-safe via Bean Validation's
 *       default null-skip behavior for {@code @Pattern}
 *   <li>Google Maps URL pattern enforces domain-anchored HTTPS URLs only
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Address request details containing location and maps link")
public final class AddressRequest {

    // ─── Regex Pattern Constants ──────────────────────────────────────────────

    /**
     * Required address line regex. Ensures at least one alphanumeric character; allows letters,
     * digits, spaces, and common punctuation (period, comma, parentheses, slash, hash, hyphen).
     * Linear-time matching — no lookaheads.
     */
    private static final String PATTERN_ADDRESS_LINE_REQUIRED =
            "^[A-Za-z0-9.,()/#\\-\\s]*[A-Za-z0-9][A-Za-z0-9.,()/#\\-\\s]*$";

    /**
     * Optional address line regex. Same character class as required variant but wrapped in {@code
     * (?:...)?} to accept empty string (for clearing the field). Null is handled by Bean
     * Validation's null-skip on {@code @Pattern}.
     */
    private static final String PATTERN_ADDRESS_LINE_OPTIONAL =
            "^(?:[A-Za-z0-9.,()/#\\-\\s]*[A-Za-z0-9][A-Za-z0-9.,()/#\\-\\s]*)?$";

    /**
     * Name field regex (city, state, country, district, taluk). Accepts letters, spaces, hyphens,
     * and periods. Used for geographic names.
     */
    private static final String PATTERN_NAME_REQUIRED = "^[A-Za-z\\s.-]+$";

    /**
     * Optional name field regex (district, taluk). Same as name required but wrapped in {@code
     * (?:...)?} for optional fields.
     */
    private static final String PATTERN_NAME_OPTIONAL = "^(?:[A-Za-z\\s.-]+)?$";

    /** Indian 6-digit pincode regex. */
    private static final String PATTERN_PINCODE = "^[0-9]{6}$";

    /**
     * Google Maps URL regex.
     *
     * <p>Accepts HTTPS URLs from:
     *
     * <ul>
     *   <li>{@code https://www.google.com/maps/...}
     *   <li>{@code https://google.com/maps/...}
     *   <li>{@code https://maps.app.goo.gl/...}
     * </ul>
     *
     * <p>FIX: Original pattern {@code ^(?:https://(www\\.)?(google\\.com/maps|
     * maps\\.app\\.goo\\.gl).*)?$} silently accepted empty strings via the outer {@code (?:...)?}
     * wrapper. Empty string is not a valid URL. This pattern requires a valid URL if a value is
     * present (null is handled by Bean Validation null-skip). The {@code [^\\s]*} path segment
     * restricts the path to non-whitespace characters only, preventing whitespace injection.
     */
    private static final String PATTERN_GOOGLE_MAPS_URL =
            "^https://(www\\.)?(?:google\\.com/maps|maps\\.app\\.goo\\.gl)[^\\s]*$";

    // ─── Fields ──────────────────────────────────────────────────────────────

    @Schema(
            description = "Primary street address line",
            example = "123 MG Road",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1,
            maxLength = 255)
    @NotBlank(
            message = "{validation.address.addressLine1.required}",
            groups = ValidationGroups.Step5Address.class)
    @Size(
            max = 255,
            message = "{validation.address.addressLine1.size}",
            groups = ValidationGroups.Step5Address.class)
    @Pattern(
            regexp = PATTERN_ADDRESS_LINE_REQUIRED,
            message = "{validation.address.addressLine1.invalid}",
            groups = ValidationGroups.Step5Address.class)
    private String addressLine1;

    @Schema(
            description = "Secondary address line (suite, building, floor)",
            example = "Apartment 4B",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            maxLength = 255)
    @Size(
            max = 255,
            message = "{validation.address.addressLine2.size}",
            groups = ValidationGroups.Step5Address.class)
    @Pattern(
            regexp = PATTERN_ADDRESS_LINE_OPTIONAL,
            message = "{validation.address.addressLine2.invalid}",
            groups = ValidationGroups.Step5Address.class)
    private String addressLine2;

    @Schema(
            description = "City name",
            example = "Bengaluru",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 2,
            maxLength = 100)
    @NotBlank(
            message = "{validation.address.city.required}",
            groups = ValidationGroups.Step5Address.class)
    @Size(
            min = 2,
            max = 100,
            message = "{validation.address.city.size}",
            groups = ValidationGroups.Step5Address.class)
    @Pattern(
            regexp = PATTERN_NAME_REQUIRED,
            message = "{validation.address.city.invalid}",
            groups = ValidationGroups.Step5Address.class)
    private String city;

    @Schema(
            description = "District name",
            example = "Bengaluru Urban",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            maxLength = 100)
    @Size(
            max = 100,
            message = "{validation.address.district.size}",
            groups = ValidationGroups.Step5Address.class)
    @Pattern(
            regexp = PATTERN_NAME_OPTIONAL,
            message = "{validation.address.district.invalid}",
            groups = ValidationGroups.Step5Address.class)
    private String district;

    @Schema(
            description = "Taluk or sub-district name",
            example = "Bengaluru North",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            maxLength = 100)
    @Size(
            max = 100,
            message = "{validation.address.taluk.size}",
            groups = ValidationGroups.Step5Address.class)
    @Pattern(
            regexp = PATTERN_NAME_OPTIONAL,
            message = "{validation.address.taluk.invalid}",
            groups = ValidationGroups.Step5Address.class)
    private String taluk;

    @Schema(
            description = "State name",
            example = "Karnataka",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 2,
            maxLength = 100)
    @NotBlank(
            message = "{validation.address.state.required}",
            groups = ValidationGroups.Step5Address.class)
    @Size(
            min = 2,
            max = 100,
            message = "{validation.address.state.size}",
            groups = ValidationGroups.Step5Address.class)
    @Pattern(
            regexp = PATTERN_NAME_REQUIRED,
            message = "{validation.address.state.invalid}",
            groups = ValidationGroups.Step5Address.class)
    private String state;

    @Schema(
            description = "Postal code / Pincode",
            example = "560001",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 6,
            maxLength = 6)
    @NotBlank(
            message = "{validation.address.pincode.required}",
            groups = ValidationGroups.Step5Address.class)
    @Size(
            min = 6,
            max = 6,
            message = "{validation.address.pincode.size}",
            groups = ValidationGroups.Step5Address.class)
    @Pattern(
            regexp = PATTERN_PINCODE,
            message = "{validation.address.pincode.pattern}",
            groups = ValidationGroups.Step5Address.class)
    private String pincode;

    @Schema(
            description = "Country name",
            example = "India",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 2,
            maxLength = 100)
    @NotBlank(
            message = "{validation.address.country.required}",
            groups = ValidationGroups.Step5Address.class)
    @Size(
            min = 2,
            max = 100,
            message = "{validation.address.country.size}",
            groups = ValidationGroups.Step5Address.class)
    @Pattern(
            regexp = PATTERN_NAME_REQUIRED,
            message = "{validation.address.country.invalid}",
            groups = ValidationGroups.Step5Address.class)
    private String country;

    @Schema(
            description = "Optional Google Maps link to the location",
            example = "https://maps.app.goo.gl/xyz",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            maxLength = 2048)
    @Size(
            max = 2048,
            message = "{validation.address.googleMapsUrl.size}",
            groups = ValidationGroups.Step5Address.class)
    @Pattern(
            regexp = PATTERN_GOOGLE_MAPS_URL,
            message = "{validation.address.googleMapsUrl.invalid}",
            groups = ValidationGroups.Step5Address.class)
    private String googleMapsUrl;
}
