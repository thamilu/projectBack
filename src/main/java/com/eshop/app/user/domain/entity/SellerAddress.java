package com.eshop.app.user.domain.entity;

import com.eshop.app.core.exception.business.DomainValidationException;
import com.eshop.app.core.kernel.DomainGuard;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.*;

/**
 * Value Object representing a physical address for seller profiles.
 *
 * <p><b>Immutable by design</b> - use factory methods to create validated instances. Direct builder
 * usage is discouraged; use factory methods instead.
 *
 * <p>Supports Indian and international address formats.
 *
 * <h3>Usage Examples:</h3>
 *
 * <pre>{@code
 * // Indian address
 * SellerAddress address = SellerAddress.createIndian(
 *     "123 MG Road", null, "Bangalore", "Bangalore Urban",
 *     "Bangalore North", "Karnataka", "560001", "India"
 * );
 *
 * // International address
 * SellerAddress intl = SellerAddress.createInternational(
 *     "123 Main St", "Apt 4B", "New York", "NY", "10001", "USA"
 * );
 *
 * // Immutable update
 * SellerAddress updated = address.withPincode("560002");
 * }</pre>
 *
 * @author Your Team
 * @version 2.3
 * @since 1.0
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA requirement
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode
public class SellerAddress {

    // ========================================================================
    // Constants
    // ========================================================================

    private static final int ADDRESS_LINE_MAX_LEN = 500;
    private static final int TEXT_FIELD_MAX_LEN = 100;
    private static final int PINCODE_MAX_LEN = 20;

    private static final String DEFAULT_COUNTRY = "India";

    // Lazy Pattern Compilation Holder (Micro-optimization)
    private static class Patterns {
        static final Pattern INDIAN_PINCODE = Pattern.compile("^[0-9]{6}$");
        static final Pattern INTERNATIONAL_POSTAL =
                Pattern.compile("^[A-Z0-9\\s-]{3,20}$", Pattern.CASE_INSENSITIVE);
    }

    // ========================================================================
    // Fields
    // ========================================================================

    @Column(name = "address_line1", length = ADDRESS_LINE_MAX_LEN, nullable = false)
    private String addressLine1;

    @Column(name = "address_line2", length = ADDRESS_LINE_MAX_LEN)
    private String addressLine2;

    @Column(name = "city", length = TEXT_FIELD_MAX_LEN, nullable = false)
    private String city;

    @Column(name = "district", length = TEXT_FIELD_MAX_LEN)
    private String district;

    @Column(name = "taluk", length = TEXT_FIELD_MAX_LEN)
    private String taluk;

    @Column(name = "state", length = TEXT_FIELD_MAX_LEN, nullable = false)
    private String state;

    @Column(name = "pincode", length = PINCODE_MAX_LEN, nullable = false)
    private String pincode;

    @Column(name = "country", length = TEXT_FIELD_MAX_LEN, nullable = false)
    private String country;

    // Lazy cache for formatted multiline address
    @Transient @ToString.Exclude @EqualsAndHashCode.Exclude
    private transient String cachedMultiLine;

    // ========================================================================
    // Factory Methods (Validated Creation) - PRIMARY API
    // ========================================================================

    /**
     * Factory method to create a validated SellerAddress (auto-detects Indian vs International).
     *
     * @param addressLine1 primary address line (required)
     * @param addressLine2 secondary address line (optional)
     * @param city city name (required)
     * @param district district name (optional, used for Indian addresses)
     * @param taluk taluk/tehsil name (optional, used for Indian addresses)
     * @param state state name (required)
     * @param pincode postal/zip code (required)
     * @param country country name (optional, defaults to "India")
     * @return validated SellerAddress instance
     * @throws DomainValidationException if validation fails
     */
    public static SellerAddress create(
            String addressLine1,
            String addressLine2,
            String city,
            String district,
            String taluk,
            String state,
            String pincode,
            String country) {

        String normalizedCountry =
                country == null || country.isBlank() ? DEFAULT_COUNTRY : country.trim();

        if (normalizedCountry.equalsIgnoreCase(DEFAULT_COUNTRY)) {
            return createIndian(
                    addressLine1,
                    addressLine2,
                    city,
                    district,
                    taluk,
                    state,
                    pincode,
                    normalizedCountry);
        } else {
            return createInternational(
                    addressLine1, addressLine2, city, state, pincode, normalizedCountry);
        }
    }

    /**
     * Creates a validated Indian address.
     *
     * @param addressLine1 primary address line (required)
     * @param addressLine2 secondary address line (optional)
     * @param city city name (required)
     * @param district district name (optional)
     * @param taluk taluk/tehsil name (optional)
     * @param state state name (required)
     * @param pincode 6-digit Indian pincode (required)
     * @param country country name (defaults to "India")
     * @return validated SellerAddress instance
     * @throws DomainValidationException if validation fails
     */
    public static SellerAddress createIndian(
            String addressLine1,
            String addressLine2,
            String city,
            String district,
            String taluk,
            String state,
            String pincode,
            String country) {

        return new SellerAddress(
                validateAndNormalize(addressLine1, "addressLine1", ADDRESS_LINE_MAX_LEN, true),
                validateAndNormalize(addressLine2, "addressLine2", ADDRESS_LINE_MAX_LEN, false),
                validateAndNormalize(city, "city", TEXT_FIELD_MAX_LEN, true),
                validateAndNormalize(district, "district", TEXT_FIELD_MAX_LEN, false),
                validateAndNormalize(taluk, "taluk", TEXT_FIELD_MAX_LEN, false),
                validateAndNormalize(state, "state", TEXT_FIELD_MAX_LEN, true),
                validateIndianPincode(pincode),
                validateAndNormalize(
                        country != null && !country.isBlank() ? country : DEFAULT_COUNTRY,
                        "country",
                        TEXT_FIELD_MAX_LEN,
                        true),
                null);
    }

    /**
     * Creates a validated international address.
     *
     * @param addressLine1 primary address line (required)
     * @param addressLine2 secondary address line (optional)
     * @param city city name (required)
     * @param state state/province name (required)
     * @param postalCode postal/zip code (required)
     * @param country country name (required)
     * @return validated SellerAddress instance
     * @throws DomainValidationException if validation fails
     */
    public static SellerAddress createInternational(
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country) {

        return new SellerAddress(
                validateAndNormalize(addressLine1, "addressLine1", ADDRESS_LINE_MAX_LEN, true),
                validateAndNormalize(addressLine2, "addressLine2", ADDRESS_LINE_MAX_LEN, false),
                validateAndNormalize(city, "city", TEXT_FIELD_MAX_LEN, true),
                null, // district not used for international
                null, // taluk not used for international
                validateAndNormalize(state, "state", TEXT_FIELD_MAX_LEN, true),
                validateInternationalPostalCode(postalCode),
                validateAndNormalize(country, "country", TEXT_FIELD_MAX_LEN, true),
                null);
    }

    /**
     * Creates a minimal valid address.
     *
     * @param addressLine1 primary address line
     * @param city city name
     * @param state state name
     * @param pincode postal code
     * @param country country name
     * @return validated SellerAddress instance
     * @throws DomainValidationException if validation fails
     */
    public static SellerAddress createMinimal(
            String addressLine1, String city, String state, String pincode, String country) {

        return create(addressLine1, null, city, null, null, state, pincode, country);
    }

    // ========================================================================
    // Validation Helpers (DRY Principle)
    // ========================================================================

    /**
     * Generic validation and normalization for text fields. <b>Core DRY implementation</b> -
     * eliminates repetitive validation code.
     *
     * @param value input value
     * @param fieldName field name for error messages
     * @param maxLength maximum allowed length
     * @param required whether field is mandatory
     * @return normalized value or null
     * @throws DomainValidationException if validation fails
     */
    private static String validateAndNormalize(
            String value, String fieldName, int maxLength, boolean required) {

        // Handle null/empty
        if (value == null || value.isBlank()) {
            if (required) {
                throw new DomainValidationException(
                        fieldName, String.format("%s is required and cannot be blank", fieldName));
            }
            return null;
        }

        // Normalize: trim and collapse multiple spaces
        String normalized = value.trim().replaceAll("\\s+", " ");

        // Validate length
        DomainGuard.requireMaxLength(normalized, fieldName, maxLength);

        return normalized;
    }

    /**
     * Validates Indian 6-digit pincode format.
     *
     * @param pincode input pincode
     * @return validated and normalized pincode
     * @throws DomainValidationException if validation fails
     */
    private static String validateIndianPincode(String pincode) {
        if (pincode == null || pincode.isBlank()) {
            throw new DomainValidationException("pincode", "Pincode is required");
        }

        String normalized = pincode.trim();

        if (!Patterns.INDIAN_PINCODE.matcher(normalized).matches()) {
            throw new DomainValidationException(
                    "pincode",
                    String.format(
                            "Invalid Indian pincode format: '%s'. Expected 6 digits.", pincode));
        }

        return normalized;
    }

    /**
     * Validates international postal code format.
     *
     * @param postalCode input postal code
     * @return validated and normalized postal code
     * @throws DomainValidationException if validation fails
     */
    private static String validateInternationalPostalCode(String postalCode) {
        if (postalCode == null || postalCode.isBlank()) {
            throw new DomainValidationException("postalCode", "Postal code is required");
        }

        String normalized = postalCode.trim().toUpperCase();

        if (!Patterns.INTERNATIONAL_POSTAL.matcher(normalized).matches()) {
            throw new DomainValidationException(
                    "postalCode", String.format("Invalid postal code format: '%s'", postalCode));
        }

        return normalized;
    }

    // ========================================================================
    // Business Methods
    // ========================================================================

    /**
     * Checks if the address has all mandatory fields populated.
     *
     * @return true if address is complete
     */
    public boolean isComplete() {
        return Stream.of(addressLine1, city, state, pincode, country)
                .allMatch(field -> field != null && !field.isBlank());
    }

    /**
     * Checks if this is an Indian address.
     *
     * @return true if country is India
     */
    public boolean isIndianAddress() {
        return country != null && country.equalsIgnoreCase(DEFAULT_COUNTRY);
    }

    /**
     * Checks if the address has district-level information (typically Indian addresses).
     *
     * @return true if district or taluk is present
     */
    public boolean hasDistrictInfo() {
        return (district != null && !district.isBlank()) || (taluk != null && !taluk.isBlank());
    }

    /**
     * Formats the address as a single-line string.
     *
     * @return formatted address (comma-separated)
     */
    public String toSingleLine() {
        return Stream.of(addressLine1, addressLine2, city, taluk, district, state, pincode, country)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    /**
     * Formats the address as multi-line string with lazy caching support.
     *
     * @return formatted multi-line address
     */
    public String toMultiLine() {
        if (cachedMultiLine == null) {
            cachedMultiLine = buildMultiLine();
        }
        return cachedMultiLine;
    }

    /** Builds the multi-line formatted address. */
    private String buildMultiLine() {
        StringBuilder sb = new StringBuilder();

        appendIfPresent(sb, addressLine1);
        appendIfPresent(sb, addressLine2);

        // City line (with optional district info for Indian addresses)
        StringBuilder cityLine = new StringBuilder();
        if (city != null) cityLine.append(city);
        if (taluk != null) cityLine.append(cityLine.length() > 0 ? ", " : "").append(taluk);
        if (district != null) cityLine.append(cityLine.length() > 0 ? ", " : "").append(district);
        appendIfPresent(sb, cityLine.toString());

        // State line
        StringBuilder stateLine = new StringBuilder();
        if (state != null) stateLine.append(state);
        if (pincode != null) stateLine.append(stateLine.length() > 0 ? " - " : "").append(pincode);
        appendIfPresent(sb, stateLine.toString());

        appendIfPresent(sb, country);

        return sb.toString();
    }

    /**
     * Gets the postal code (alias for pincode).
     *
     * @return postal code
     */
    public String getPostalCode() {
        return pincode;
    }

    /**
     * Gets the primary address line as Optional.
     *
     * @return Optional containing addressLine1
     */
    public Optional<String> getPrimaryAddressLine() {
        return Optional.ofNullable(addressLine1);
    }

    /**
     * Gets the secondary address line as Optional.
     *
     * @return Optional containing addressLine2
     */
    public Optional<String> getSecondaryAddressLine() {
        return Optional.ofNullable(addressLine2);
    }

    // ========================================================================
    // Immutable Update Methods
    // ========================================================================

    /**
     * Creates a new instance with updated address line 1.
     *
     * @param newAddressLine1 new address line 1
     * @return new SellerAddress instance
     * @throws DomainValidationException if validation fails
     */
    public SellerAddress withAddressLine1(String newAddressLine1) {
        return new SellerAddress(
                validateAndNormalize(newAddressLine1, "addressLine1", ADDRESS_LINE_MAX_LEN, true),
                this.addressLine2,
                this.city,
                this.district,
                this.taluk,
                this.state,
                this.pincode,
                this.country,
                null);
    }

    /**
     * Creates a new instance with updated address line 2.
     *
     * @param newAddressLine2 new address line 2
     * @return new SellerAddress instance
     * @throws DomainValidationException if validation fails
     */
    public SellerAddress withAddressLine2(String newAddressLine2) {
        return new SellerAddress(
                this.addressLine1,
                validateAndNormalize(newAddressLine2, "addressLine2", ADDRESS_LINE_MAX_LEN, false),
                this.city,
                this.district,
                this.taluk,
                this.state,
                this.pincode,
                this.country,
                null);
    }

    /**
     * Creates a new instance with updated city.
     *
     * @param newCity new city
     * @return new SellerAddress instance
     * @throws DomainValidationException if validation fails
     */
    public SellerAddress withCity(String newCity) {
        return new SellerAddress(
                this.addressLine1,
                this.addressLine2,
                validateAndNormalize(newCity, "city", TEXT_FIELD_MAX_LEN, true),
                this.district,
                this.taluk,
                this.state,
                this.pincode,
                this.country,
                null);
    }

    /**
     * Creates a new instance with updated pincode. Automatically validates based on country (Indian
     * vs International).
     *
     * @param newPincode new pincode
     * @return new SellerAddress instance
     * @throws DomainValidationException if validation fails
     */
    public SellerAddress withPincode(String newPincode) {
        String validatedPincode =
                isIndianAddress()
                        ? validateIndianPincode(newPincode)
                        : validateInternationalPostalCode(newPincode);

        return new SellerAddress(
                this.addressLine1,
                this.addressLine2,
                this.city,
                this.district,
                this.taluk,
                this.state,
                validatedPincode,
                this.country,
                null);
    }

    /**
     * Creates a new instance with updated state.
     *
     * @param newState new state
     * @return new SellerAddress instance
     * @throws DomainValidationException if validation fails
     */
    public SellerAddress withState(String newState) {
        return new SellerAddress(
                this.addressLine1,
                this.addressLine2,
                this.city,
                this.district,
                this.taluk,
                validateAndNormalize(newState, "state", TEXT_FIELD_MAX_LEN, true),
                this.pincode,
                this.country,
                null);
    }

    // ========================================================================
    // Private Helpers
    // ========================================================================

    /**
     * Appends a value to StringBuilder if present (DRY helper for formatting).
     *
     * @param sb StringBuilder to append to
     * @param value value to append
     */
    private void appendIfPresent(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(value);
        }
    }
}
