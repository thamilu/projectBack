package com.eshop.app.seller.api.request.validation;

/**
 * Interface contract for objects that carry GST registration status and GSTIN (GST Identification
 * Number) data, enabling cross-field validation via the {@link ValidGstinIfRegistered} constraint.
 *
 * <h3>Purpose</h3>
 *
 * <p>This interface is a <em>validation contract marker</em>. It exposes exactly the two fields
 * required by {@link GstinConditionalValidator} to evaluate GST consistency rules — no more, no
 * less (Interface Segregation).
 *
 * <h3>Validation Rules Enforced</h3>
 *
 * <p>When a class implements this interface AND is annotated with {@link ValidGstinIfRegistered},
 * the following rules are enforced by {@link GstinConditionalValidator}:
 *
 * <ol>
 *   <li><strong>Required when registered:</strong> {@code getGstRegistered() == true} requires a
 *       non-blank {@code getGstin()}
 *   <li><strong>Prohibited when not registered:</strong> {@code getGstRegistered() != true}
 *       requires {@code getGstin()} to be {@code null} or blank — a GSTIN submitted alongside an
 *       unregistered status is a contradictory data state
 * </ol>
 *
 * <h3>Implementation Obligations</h3>
 *
 * <p>Classes implementing this interface <strong>must</strong> also be annotated with {@link
 * ValidGstinIfRegistered} to activate the cross-field validation. Implementing the interface
 * without the annotation is permitted (e.g., for non-validated command objects) but will not
 * trigger any validation.
 *
 * <p>Example implementation:
 *
 * <pre>{@code
 * @ValidGstinIfRegistered(groups = ValidationGroups.Step2Kyc.class)
 * public class SellerRegisterRequest implements GstinValidatable {
 *
 *     private Boolean gstRegistered;
 *
 *     @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")
 *     private String gstin;
 *
 *     {@literal @}Override
 *     public Boolean getGstRegistered() { return gstRegistered; }
 *
 *     {@literal @}Override
 *     public String getGstin() { return gstin; }
 * }
 * }</pre>
 *
 * <h3>Null and Empty String Semantics</h3>
 *
 * <p>Implementors must follow these conventions to ensure correct validator behavior:
 *
 * <ul>
 *   <li>{@link #getGstRegistered()} — return {@code null} when the field was not submitted by the
 *       caller. Do not substitute {@code Boolean.FALSE} for {@code null}; the validator treats them
 *       equivalently, but {@code null} is the canonical representation of "not provided".
 *   <li>{@link #getGstin()} — return {@code null} when no GSTIN was submitted. Do not return empty
 *       string {@code ""} as a substitute for {@code null}; prefer returning {@code null} for
 *       absent values. The validator treats both as "blank", but {@code null} is the canonical
 *       representation.
 * </ul>
 *
 * @see ValidGstinIfRegistered
 * @see GstinConditionalValidator
 * @since 1.0
 */
public interface GstinValidatable {

    /**
     * Returns whether the seller is registered under GST.
     *
     * <p>Return value semantics:
     *
     * <ul>
     *   <li>{@code Boolean.TRUE} — the seller has explicitly declared GST registration; a valid
     *       GSTIN is required
     *   <li>{@code Boolean.FALSE} — the seller has explicitly declared they are NOT GST registered;
     *       a GSTIN must not be provided
     *   <li>{@code null} — the field was not submitted by the caller (treated as "not registered"
     *       by {@link GstinConditionalValidator}); a GSTIN must not be provided
     * </ul>
     *
     * <p><strong>Implementation note:</strong> Return {@code null} (not {@code Boolean.FALSE}) when
     * the field was absent from the request. This preserves the distinction between "explicitly
     * unregistered" and "not declared" for downstream processing, even though the validator treats
     * both as equivalent.
     *
     * @return {@code Boolean.TRUE} if explicitly GST registered; {@code Boolean.FALSE} if
     *     explicitly not registered; {@code null} if registration status was not provided
     */
    Boolean getGstRegistered();

    /**
     * Returns the seller's GSTIN (GST Identification Number).
     *
     * <p>Return value semantics:
     *
     * <ul>
     *   <li>Non-null, non-blank string — the GSTIN value submitted by the caller; must conform to
     *       the standard 15-character GSTIN format when {@link #getGstRegistered()} is {@code
     *       Boolean.TRUE}
     *   <li>{@code null} — no GSTIN was submitted (preferred representation for absence)
     *   <li>Blank string ({@code ""} or whitespace-only) — treated equivalently to {@code null} by
     *       {@link GstinConditionalValidator}; however, implementations should prefer returning
     *       {@code null} for absent values rather than empty string
     * </ul>
     *
     * <p><strong>Security note:</strong> GSTIN is a tax identification number and constitutes
     * sensitive financial data. Implementations must ensure this value is excluded from {@code
     * toString()}, logging, and serialization outputs where PII exposure is a concern (e.g., via
     * {@code @ToString.Exclude} in Lombok).
     *
     * @return the GSTIN string if provided; {@code null} if not provided
     */
    String getGstin();
}
