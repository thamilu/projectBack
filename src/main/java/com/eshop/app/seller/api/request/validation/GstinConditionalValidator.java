package com.eshop.app.seller.api.request.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Cross-field constraint validator that enforces GSTIN consistency rules relative to the seller's
 * declared GST registration status.
 *
 * <h3>Validation Rules</h3>
 *
 * <ol>
 *   <li><strong>Required when registered:</strong> If {@code gstRegistered == true}, then {@code
 *       gstin} must be non-null and non-blank. Violation is reported on the {@code gstin} property
 *       node.
 *   <li><strong>Prohibited when not registered:</strong> If {@code gstRegistered == false} (or
 *       {@code null}), then {@code gstin} must be null or blank. A non-blank GSTIN submitted
 *       alongside {@code gstRegistered = false} is a contradictory data state that violates tax
 *       compliance data integrity. Violation is reported on the {@code gstin} property node.
 * </ol>
 *
 * <h3>Null Handling</h3>
 *
 * <ul>
 *   <li>A {@code null} root object is considered valid per JSR-380 convention — null-checking is
 *       the responsibility of {@code @NotNull} on the field.
 *   <li>{@code gstRegistered = null} is treated as {@code false} (not registered) via null-safe
 *       {@link Boolean#TRUE} equals comparison.
 * </ul>
 *
 * <h3>Thread Safety</h3>
 *
 * This validator is stateless — it holds no instance fields. Bean Validation may share a single
 * instance across concurrent validation calls. No synchronization is required.
 *
 * <h3>Spring Integration</h3>
 *
 * This class does not require {@code @Component} — it has no Spring-managed dependencies and is
 * instantiated directly by the {@code ValidatorFactory}. If a Spring-managed dependency (e.g., a
 * Repository) is needed in the future, add {@code @Component} and configure {@code
 * SpringConstraintValidatorFactory}.
 *
 * @see ValidGstinIfRegistered
 * @see GstinValidatable
 */
public final class GstinConditionalValidator
        implements ConstraintValidator<ValidGstinIfRegistered, GstinValidatable> {

    /**
     * Validates GSTIN consistency against the declared GST registration status.
     *
     * <p>Validation logic:
     *
     * <ul>
     *   <li>If {@code gstRegistered == true} and GSTIN is blank → invalid (GSTIN required when
     *       registered)
     *   <li>If {@code gstRegistered != true} and GSTIN is non-blank → invalid (GSTIN must not be
     *       provided when not registered — contradictory state)
     *   <li>All other combinations → valid
     * </ul>
     *
     * <p>Constraint violations are reported on the {@code gstin} property node to enable
     * field-level error display in API responses and UI forms.
     *
     * @param value the object implementing {@link GstinValidatable}; may be {@code null}
     * @param context the validator context for building custom constraint violations
     * @return {@code true} if the GSTIN state is consistent with the registration flag; {@code
     *     false} otherwise
     */
    @Override
    public boolean isValid(GstinValidatable value, ConstraintValidatorContext context) {
        // Per JSR-380: null root objects are always valid.
        // Null-checking of the root object is the responsibility of @NotNull.
        if (value == null) {
            return true;
        }

        // Null-safe boolean evaluation:
        // Boolean.TRUE.equals() returns false for both null and Boolean.FALSE,
        // correctly treating an unset gstRegistered as "not registered".
        boolean gstRegistered = Boolean.TRUE.equals(value.getGstRegistered());

        // Extract to local variable — avoids repeated interface method invocations
        // and makes the null-check + blank-check chain clearly readable.
        String gstin = value.getGstin();
        boolean gstinBlank = gstin == null || gstin.isBlank();

        if (gstRegistered && gstinBlank) {
            // Case 1: Seller declared as GST registered but provided no GSTIN.
            // This is the primary validation case — GSTIN is required.
            return buildViolation(context, "{validation.gstin.requiredWhenRegistered}", "gstin");
        }

        if (!gstRegistered && !gstinBlank) {
            // Case 2: Seller declared as NOT GST registered but provided a GSTIN.
            // This is a contradictory data state — a GSTIN implies GST registration.
            // Storing a GSTIN against an unregistered seller violates tax compliance
            // data integrity and must be rejected at the API boundary.
            return buildViolation(
                    context, "{validation.gstin.notAllowedWhenNotRegistered}", "gstin");
        }

        return true;
    }

    /**
     * Builds a custom constraint violation on a specific property node.
     *
     * <p>Disables the default constraint violation (which would use the annotation-level message on
     * the class) and replaces it with a field-targeted violation. This ensures that API consumers
     * receive a field-level error pointing to {@code gstin} rather than a class-level error with no
     * field association.
     *
     * <p>Always returns {@code false} — extracted as a method to eliminate the duplicate {@code
     * context.disableDefaultConstraintViolation()} call that would otherwise appear in each
     * validation branch.
     *
     * @param context the validator context
     * @param messageTemplate the message template key (e.g., {@code {validation.gstin.key}})
     * @param propertyNode the property name on which to report the violation
     * @return {@code false} always — signals constraint violation to the framework
     */
    private boolean buildViolation(
            ConstraintValidatorContext context, String messageTemplate, String propertyNode) {

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(messageTemplate)
                .addPropertyNode(propertyNode)
                .addConstraintViolation();
        return false;
    }
}
